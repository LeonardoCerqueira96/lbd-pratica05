/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package aula05.oracleinterface;

import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 *
 * @author junio
 */
public class DBFuncionalidades {
    Connection connection;
    Statement stmt;
    ResultSet rs;
    JTextArea jtAreaDeStatus;
    
    public DBFuncionalidades(JTextArea jtaTextArea){
        jtAreaDeStatus = jtaTextArea;
    }
    
    public boolean conectar(){       
        try {
            DriverManager.registerDriver (new oracle.jdbc.OracleDriver());
            connection = DriverManager.getConnection(
                    "jdbc:oracle:thin:@grad.icmc.usp.br:15215:orcl",
                    "L8937483",
                    "Furukawa*Nagisa18");
            return true;
        } catch(SQLException ex){
            jtAreaDeStatus.setText("Problema: verifique seu usuário e senha");
        }
        return false;
    }
    public void pegarNomesDeTabelas(JComboBox jc){
        try {
            this.getTableNames();
            while (rs.next()) {
                jc.addItem(rs.getString("table_name"));
            }
            stmt.close();
        } catch (SQLException ex) {
            jtAreaDeStatus.setText("Erro na consulta");
        }    
    }
    
    public void getTableNames() {
        String s = "SELECT table_name FROM user_tables";
        try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery(s);
        } catch (SQLException ex) {
            Logger.getLogger(DBFuncionalidades.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void pegarMetadadosColunas(String sTableName) {
        String sql = 
        "SELECT C.COLUMN_NAME, C.DATA_TYPE, C.NULLABLE, C.DATA_LENGTH " +
        "   FROM USER_TAB_COLUMNS C " +
        "   WHERE C.TABLE_NAME = '" + sTableName + "'";
        try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery(sql);
        } catch (SQLException e) {
            jtAreaDeStatus.setText("Erro na consulta: \"" + sql + "\" - " + e.getMessage());
        }
    }
    
    public ResultSet pegarRestricoesDeColuna(String sTableName, String sColumnName, Statement statement) {
        String sql = 
        "SELECT A.TABLE_NAME, A.COLUMN_NAME, A.CONSTRAINT_NAME, C.CONSTRAINT_TYPE, C.SEARCH_CONDITION," +
        "C_PK.TABLE_NAME R_TABLE_NAME, A_PK.COLUMN_NAME A_CL" +
        "   FROM USER_CONS_COLUMNS A" +
        "   JOIN USER_CONSTRAINTS C ON A.CONSTRAINT_NAME = C.CONSTRAINT_NAME" +
        "   LEFT OUTER JOIN USER_CONSTRAINTS C_PK ON C.R_CONSTRAINT_NAME = C_PK.CONSTRAINT_NAME" +
        "   LEFT OUTER JOIN USER_CONS_COLUMNS A_PK ON C_PK.CONSTRAINT_NAME = A_PK.CONSTRAINT_NAME" + 
        "   WHERE A.TABLE_NAME = '" + sTableName + "'" +
        "       AND A.COLUMN_NAME = '" + sColumnName + "'";
        ResultSet res;
        try {
            res = statement.executeQuery(sql);
        } catch (SQLException e) {
            jtAreaDeStatus.setText("Erro na consulta: \"" + sql + "\" - " + e.getMessage());
            return null;
        }
        
        return res;
    }
    
    public void exibeDados(JTable tATable, String sTableName){
        /*Aqui preencho a tabela com os dados*/
    }
    //public void preencheComboBoxComRestricoesDeCheck
    //public void preencheComboBoxComValoresReferenciados
    //
    
    public void exibirMetadadosColunas(String sTableName) {
        jtAreaDeStatus.setText("NOME\tTIPO\tPODE NULL\tTAMANHO\n\n");
        pegarMetadadosColunas(sTableName);
        try {
            while (rs.next()) {
                jtAreaDeStatus.append( 
                    rs.getString("COLUMN_NAME") + "\t" +
                    rs.getString("DATA_TYPE") + "\t" +
                    rs.getString("NULLABLE") + "\t" +
                    rs.getString("DATA_LENGTH") + "\n"
                );
            }
            stmt.close();
        } catch (SQLException e) {
            jtAreaDeStatus.setText("Erro: \"" + e + "\"");
        }
    }
    
    public void criarColunasDeInsercao(JPanel pPanelDeInsercao, String sTableName) {
        pegarMetadadosColunas(sTableName);
        
        pPanelDeInsercao.removeAll();
        pPanelDeInsercao.revalidate();
        pPanelDeInsercao.repaint();
        
        try {
            int size = 0;
            while (rs.next()) {
                Statement statement = connection.createStatement();
                String sColName = rs.getString("COLUMN_NAME");
                ResultSet res = pegarRestricoesDeColuna(sTableName, sColName, statement);
                
                while (res.next()) {
                    String type = res.getString("CONSTRAINT_TYPE");
                    if ("C".equals(type)) {
                        String incheck_regex = "^\\s*\\w+\\s+IN\\s+\\(((?:\\s*'?\\w+'?,?\\s*)+)\\)$";
                        Pattern incheck_pattern = Pattern.compile(incheck_regex);
                        
                        Matcher incheck_matcher = incheck_pattern.matcher(res.getString("SEARCH_CONDITION"));
                        if (incheck_matcher.find()) {
                            System.out.println("Found value: " + incheck_matcher.group(1).replace("'", ""));
                        } else {
                            break;
                        }
                    }
                }
                statement.close();
                
                pPanelDeInsercao.add(new JLabel(sColName));
                pPanelDeInsercao.add(new JTextField("Digite aqui..."));
                size++;
            }
            pPanelDeInsercao.setLayout(new GridLayout(size, 2));
        } catch (SQLException e) {
            jtAreaDeStatus.setText("Erro: \"" + e + "\"");
        }
    }
}
