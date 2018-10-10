/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package aula05.oracleinterface;

import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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
            rs.close();
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
    
    public ResultSet pegarMetadadosColunas(String sTableName, Statement statement) {
        String sql = 
        "SELECT C.COLUMN_NAME, C.DATA_TYPE, C.NULLABLE, C.DATA_LENGTH " +
        "   FROM USER_TAB_COLUMNS C " +
        "   WHERE C.TABLE_NAME = '" + sTableName + "'";
        ResultSet res;
        try {
            res = statement.executeQuery(sql);
        } catch (SQLException e) {
            jtAreaDeStatus.setText("Erro na consulta: \"" + sql + "\" - " + e.getMessage());
            return null;
        }
        
        return res;
    }
    
    public ResultSet pegarRestricoesDeColuna(String sTableName, String sColumnName, Statement statement) {
        String sql = 
        "SELECT A.TABLE_NAME, A.COLUMN_NAME, A.CONSTRAINT_NAME, C.CONSTRAINT_TYPE, C.SEARCH_CONDITION," +
        "C_PK.TABLE_NAME R_TABLE_NAME, A_PK.COLUMN_NAME A_CL" +
        "   FROM USER_CONS_COLUMNS A" +
        "   JOIN USER_CONSTRAINTS C ON A.CONSTRAINT_NAME = C.CONSTRAINT_NAME" +
        "   LEFT OUTER JOIN USER_CONSTRAINTS C_PK ON C.R_CONSTRAINT_NAME = C_PK.CONSTRAINT_NAME" +
        "   LEFT OUTER JOIN USER_CONS_COLUMNS A_PK ON C_PK.CONSTRAINT_NAME = A_PK.CONSTRAINT_NAME AND A.POSITION = A_PK.POSITION" + 
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
    
    public ResultSet pegarValoresChaveEstrangeira(String sPkTable, String sPkColumn, Statement statement) {
        String sql = "SELECT DISTINCT T." + sPkColumn + " FROM " + sPkTable + " T"; 
        ResultSet res;
        try {
            res = statement.executeQuery(sql);
        } catch (SQLException e) {
            jtAreaDeStatus.setText("Erro na consulta: \"" + sql + "\" - " + e.getMessage());
            return null;
        }
        
        return res;
    }
    
    public boolean checarValidadeChaveComposta(String sPkTable, List<String> sPkColumns, List<String> values, Statement statement) {
        String sql = "SELECT * FROM " + sPkTable + " T WHERE ";
        for (int i = 0; i < sPkColumns.size(); i++) {
            sql += sPkColumns.get(i) + " = '" + values.get(i) + "'";
            sql += ((i < (sPkColumns.size() - 1)) ? " AND " : ""); 
        }
                
        boolean pairPresent = false;
        ResultSet res;
        try {
            res = statement.executeQuery(sql);
            while(res.next()) { pairPresent = true; }
            res.close();
        } catch (SQLException e) {
            jtAreaDeStatus.setText("Erro na consulta: \"" + sql + "\" - " + e.getMessage());
        }
        
        return pairPresent;
    }
    
    public List<String> pegarValoresDeInsercao(JPanel panel, int numComp) {
        List<String> values = new ArrayList<String>();
        for (int i = 0; i < numComp; i++) {
            if (panel.getComponent(i * 2 + 1) instanceof JTextField) {
                JTextField comp = (JTextField) panel.getComponent(i * 2 + 1);
                values.add(comp.getText());
            } else {
                JComboBox comp = (JComboBox) panel.getComponent(i * 2 + 1);
                values.add((String) comp.getSelectedItem());
            }
        }
        return values;
    }
    
    public boolean inserirDados(String sTableName, List<String> values) {
        String sql = "INSERT INTO " + sTableName + " (";
        try {
            Statement colInfoStmt = connection.createStatement();
            ResultSet colInfoRes = pegarMetadadosColunas(sTableName, colInfoStmt);
            
            //List<String> cols = new ArrayList<String>();
            List<String> types = new ArrayList<String>();
            while (colInfoRes.next()) {
                //cols.add(colInfoRes.getString("COLUMN_NAME"));
                sql += colInfoRes.getString("COLUMN_NAME") + ", ";
                types.add(colInfoRes.getString("DATA_TYPE"));
            }
            sql = sql.substring(0, sql.length() - 2) + ") VALUES (";
            for (int i = 0; i < types.size(); i++) {
                if (types.get(i).equals("BLOB")) {
                    sql += "EMPTY_BLOB(), ";
                }
                else if (types.get(i).equals("DATE")) {
                    sql += "TO_DATE('" + values.get(i) + "', 'DD/MM/RR'), ";
                }
                else if (values.get(i).equals("")){
                    sql += "NULL, ";
                }
                else {
                    sql += "'" + values.get(i) + "', ";
                }
            }
            sql = sql.substring(0, sql.length() - 2) + ")";
            colInfoRes.close();
            colInfoStmt.close();
            
            Statement insertStmt = connection.createStatement();
            insertStmt.executeUpdate(sql);
        } catch (SQLException e) {
            jtAreaDeStatus.setText("Erro: \"" + e + "\"");
            return false;
        }
        return true;
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
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            jtAreaDeStatus.setText("Erro: \"" + e + "\"");
        }
    }
    
    public void criarColunasDeInsercao(JPanel pPanelDeInsercao, final String sTableName) {
        pegarMetadadosColunas(sTableName);
        
        pPanelDeInsercao.removeAll();
        pPanelDeInsercao.revalidate();
        pPanelDeInsercao.repaint();
        
        try {
            int size = 0;
            Map<String, List<List<String>>> foreignKeys = new HashMap<String, List<List<String>>>();
            while (rs.next()) {
                String sColName = rs.getString("COLUMN_NAME");
                
                Statement consStmt = connection.createStatement();
                ResultSet consRs = pegarRestricoesDeColuna(sTableName, sColName, consStmt);
                
                JComboBox insertCb = new JComboBox();
                boolean addComboBox = false;
                while (consRs.next()) {
                    boolean gotForeignValues = false;
                    String type = consRs.getString("CONSTRAINT_TYPE");
                    if ("C".equals(type)) {
                        String searchCondition = consRs.getString("SEARCH_CONDITION");
                        String incheckRegex = "^\\s*\\w+\\s+IN\\s+\\(((?:\\s*'?\\w+'?,?\\s*)+)\\)$";
                        Pattern incheckPattern = Pattern.compile(incheckRegex);
                        
                        Matcher incheckMatcher = incheckPattern.matcher(searchCondition);
                        if (incheckMatcher.find()) {
                            addComboBox = true;
                            
                            String[] values = incheckMatcher.group(1).replaceAll("'", "").replaceAll("\\s+", "").split(",");
                            for (String value : values) {
                                insertCb.addItem(value);
                            }
                        }
                    }
                    else if ("R".equals(type)) {
                        addComboBox = true;
                        
                        String sConsName = consRs.getString("CONSTRAINT_NAME");
                        String pkTable = consRs.getString("R_TABLE_NAME");
                        String pkCl = consRs.getString("A_CL");
                        
                        List<String> fkRefs = new ArrayList<String>();
                        if (!foreignKeys.containsKey(sConsName)) {
                            foreignKeys.put(sConsName, new ArrayList<List<String>>());
                        }
                        fkRefs.add(Integer.toString(size));
                        fkRefs.add(pkTable);
                        fkRefs.add(pkCl);
                        foreignKeys.get(sConsName).add(fkRefs);
                        
                        if (!gotForeignValues) {
                            Statement fkStmt = connection.createStatement();
                            ResultSet fkRs = pegarValoresChaveEstrangeira(pkTable, pkCl, fkStmt);
                            while (fkRs.next()) {
                                insertCb.addItem(fkRs.getString(pkCl));
                            }
                            fkRs.close();
                            fkStmt.close();
                            
                            gotForeignValues = true;
                        }
                    }
                }
                pPanelDeInsercao.add(new JLabel(sColName + (rs.getString("NULLABLE").equals("N") ? "*" : "")));
                pPanelDeInsercao.add(addComboBox ? insertCb : new JTextField("Digite aqui..."));
                
                consRs.close();
                consStmt.close();
                    
                size++;
            }
            JButton botaoInserir = new JButton("Inserir");
            botaoInserir.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    JPanel panel = (JPanel) ((JButton) e.getSource()).getParent();
                    List<String> values = pegarValoresDeInsercao(panel, (panel.getComponentCount() / 2) - 1);
                    boolean success = inserirDados(sTableName, values);
                    ((JLabel) panel.getComponent(panel.getComponentCount() - 1))
                        .setText(success ? "Inserção feita com êxito" : "Inserção falhou");
                    if (success) { exibirMetadadosColunas(sTableName); }   
                }
            });
            pPanelDeInsercao.add(botaoInserir);
            pPanelDeInsercao.add(new JLabel());
            pPanelDeInsercao.setLayout(new GridLayout(size + 1, 15, 2, 2));
            
            final List<JComboBox> cbs = new ArrayList<JComboBox>();
            final List<String> pkCls = new ArrayList<String>();
            final List<String> pkTable = new ArrayList<String>();
            Iterator outerIt = foreignKeys.entrySet().iterator();
            while (outerIt.hasNext()) {
                Map.Entry pair = (Map.Entry)outerIt.next();
                for (List<String> fkRefs : (List<List<String>>) pair.getValue()) {
                    cbs.add((JComboBox)(pPanelDeInsercao.getComponent(Integer.parseInt(fkRefs.get(0)) * 2 + 1)));
                    pkTable.add(fkRefs.get(1));
                    pkCls.add(fkRefs.get(2));
                }
                if (pkCls.size() < 2) { continue; }
                for (JComboBox cb : cbs) { 
                    cb.addItemListener(new java.awt.event.ItemListener() {
                        public void itemStateChanged(java.awt.event.ItemEvent evt) {
                            if (evt.getStateChange() == ItemEvent.SELECTED)
                            {
                                List<String> values = new ArrayList<String>();
                                for (JComboBox v : cbs) {
                                    values.add((String) v.getSelectedItem());
                                }
                                try {
                                    Statement checkPairStmt = connection.createStatement();
                                    boolean hasPair = checarValidadeChaveComposta(pkTable.get(0), pkCls, values, checkPairStmt);
                                    checkPairStmt.close();
                                    if (!hasPair) {
                                        jtAreaDeStatus.setText("Erro: Combinacao " + values + "nao existe");
                                    } else {
                                        exibirMetadadosColunas(pkTable.get(0));
                                    }
                                } catch (SQLException e) {
                                    jtAreaDeStatus.setText("Erro: \"" + e + "\"");
                                }
                            }
                        }
                    });
                }
            }
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            jtAreaDeStatus.setText("Erro: \"" + e + "\"");
        }
    }
}
