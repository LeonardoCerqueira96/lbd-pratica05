/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package aula05.oracleinterface;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextArea;

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
                    "jdbc:oracle:thin:@192.168.183.15:1521:orcl",
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
    
    public void exibeDados(JTable tATable, String sTableName){
        /*Aqui preencho a tabela com os dados*/
    }
    //public void preencheComboBoxComRestricoesDeCheck
    //public void preencheComboBoxComValoresReferenciados
    //
    
    public void exibirMetadadosColunas(String sTableName) {
        jtAreaDeStatus.setText("Colunas da tabela " + sTableName + ": \n");
        pegarMetadadosColunas(sTableName);
        try {
            while (rs.next()) {
                jtAreaDeStatus.append( 
                    rs.getString("COLUMN_NAME") + ": " +
                    rs.getString("DATA_TYPE") + ", " +
                    rs.getString("NULLABLE") + ", " +
                    rs.getString("DATA_LENGTH") + "\n"
                );
            }
            stmt.close();
        } catch (SQLException e) {
            jtAreaDeStatus.setText("Erro: \"" + e + "\"");
        }
    }
}
