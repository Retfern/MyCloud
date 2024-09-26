package swing;

import client.Network;
import common.message.CommandMessage;
import common.message.InfoFileClass;
import common.message.NewChanelForSendFileMessage;
import common.StatusFile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import java.util.Map;


public class MainWindow extends JFrame {

    private final String CLIENT_STORAGE = "client_storage/";
    private JButton downloadFile;
    private JButton addNewFileOnServer;
    private JButton deleteFile;
    private JButton myStorage;
    private JFileChooser fileChooserForDownloadOnServer;
    private JPanel buttonPanel;
    private ModelListFile table;
    private Network network;
    private LoginDialog loginDialog;
    private String userName = null;
    private Map <String, Network> listDownload = new HashMap();
    private Map <String, StatusFile> listForDeletFile = new HashMap();

    public MainWindow() {
        setTitle("Облачное хранилище");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setBounds(200, 200, 500, 500);
        setLayout(new BorderLayout());
        buttonPanel = new JPanel();
        Box contents = new Box(BoxLayout.Y_AXIS);
        table = new ModelListFile();
        contents.add(new JScrollPane(table.getTable()));
        add(contents, BorderLayout.CENTER);

        downloadFile = new JButton("Скачать");
        addNewFileOnServer = new JButton("Загрузить");
        deleteFile = new JButton("Удалить");
        myStorage = new JButton("Загрузки");
        fileChooserForDownloadOnServer = new JFileChooser();

        GridLayout layout = new GridLayout(0, 4, 5, 12);
        buttonPanel.setLayout(layout);
        buttonPanel.add (downloadFile);
        buttonPanel.add (addNewFileOnServer);
        buttonPanel.add (deleteFile);
        buttonPanel.add (myStorage);
        add(buttonPanel, BorderLayout.SOUTH);

       addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                if (network != null) {
                    if (listDownload.isEmpty()){
                        network.stop();
                        try {
                            network.getThread().join();
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                        super.windowClosing(e);

                    }else {
                        try {
                            if (showMessageForCloseWindow()) {
                                network.stop();
                                network.getThread().join();
                                setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                                super.windowClosing(e);
                            }else {
                                System.out.println("продолжить загрузку");
                            }
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }

        );

        setVisible(true);
        network = new Network(this);
        loginDialog = new LoginDialog(this, network);
        loginDialog.setVisible(true);

        if (!loginDialog.isConnected()) {
            System.exit(0);
        }

        downloadFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                int idx = table.getTable().getSelectedRow();
                if (idx==-1) {
                    JOptionPane.showMessageDialog(MainWindow.this,
                              "Выберите файл для скачивания",
                              "Внимание!",
                              JOptionPane.PLAIN_MESSAGE);
                }else {
                    String fileName = (String) table.getTable().getValueAt(idx, 0);

                    String[] subStr;
                    String str =(String) table.getTable().getValueAt(idx, 1);
                    String delimeter = " byte";
                    subStr = str.split(delimeter);

                    long size = Long.valueOf(subStr[0]);

                    File fileExist = new File (CLIENT_STORAGE + userName + "/"+fileName);
                    if (fileExist.exists()) {
                        boolean result = showMessageForLocalStorage(fileName);
                        if (result) {
                            if (fileExist.delete()){
                                newNetworkForDownloadFile (fileName, size);
                            }
                        }else {
                            System.out.println("Выбрана отмена");
                        }
                    }else {
                        File dir = new File(CLIENT_STORAGE + userName);
                        if (!dir.exists()) {
                            dir.mkdir();
                        }
                        newNetworkForDownloadFile (fileName, size);
                    }
                }
            }
        });

        deleteFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                Integer idx = table.getTable().getSelectedRow();
                if (idx==-1) {
                    JOptionPane.showMessageDialog(MainWindow.this,
                              "Выберите файл для скачивания",
                              "Внимание!",
                              JOptionPane.PLAIN_MESSAGE);
                }else {
                    String fileName = (String) table.getTable().getValueAt(idx, 0);
                    if (!listDownload.containsKey(fileName)) {
                        network.sendMessage(new CommandMessage(CommandMessage.Command.DELETE, fileName, idx));
                    }else {
                        try {
                            showMessageDeleteDownloadFile(fileName, idx);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }

            }
        });

        addNewFileOnServer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileChooserForDownloadOnServer.setDialogTitle("Выбор файла");
                fileChooserForDownloadOnServer.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                fileChooserForDownloadOnServer.showOpenDialog(MainWindow.this);

                String path = fileChooserForDownloadOnServer.getSelectedFile().getPath();
                InfoFileClass fileData = new InfoFileClass(path, fileChooserForDownloadOnServer.getSelectedFile().getName(), fileChooserForDownloadOnServer.getSelectedFile().length(), StatusFile.SEND);
                Network networkAddNewFileOnServer = new Network(MainWindow.this, fileData);
                listDownload.put(fileChooserForDownloadOnServer.getSelectedFile().getName(), networkAddNewFileOnServer);
                networkAddNewFileOnServer.changePipeline();
                networkAddNewFileOnServer.sendMessage(new NewChanelForSendFileMessage(userName));
                networkAddNewFileOnServer.sendMessage(new CommandMessage(CommandMessage.Command.ADD, fileChooserForDownloadOnServer.getSelectedFile().getName(), fileChooserForDownloadOnServer.getSelectedFile().length()));
            }
        });


        myStorage.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser chooser = new JFileChooser();
                chooser.setCurrentDirectory(
                        chooser.getFileSystemView().getParentDirectory(
                                new File(CLIENT_STORAGE + userName)));
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.showDialog(myStorage, "Open file");
                String path = chooser.getSelectedFile().getPath();

                Desktop desktop = null;
                if (Desktop.isDesktopSupported()) {
                    desktop = Desktop.getDesktop();
                }

                try {
                    desktop.open(new File (path));
                } catch (IOException ioe) {
                    System.out.println("IOException Desktop");
                    ioe.printStackTrace();
                }
            }
        });
    }

    public LoginDialog getLoginDialog(){
        return loginDialog;
    }

    public ModelListFile getTableFiles () {
        return table;
    }

    public void setUserName (String name){
        this.userName = name;
    }

    private void newNetworkForDownloadFile (String fileName, long size) {

        InfoFileClass infoFile = new InfoFileClass(CLIENT_STORAGE + userName+"/"+fileName, fileName, size, StatusFile.DOWNLOAD);
        Network networkDownloadFile = new Network(MainWindow.this, infoFile);
        listDownload.put(fileName, networkDownloadFile);
        System.out.println("networkDownloadFile username " + userName);
        networkDownloadFile.changePipeline();
        networkDownloadFile.sendMessage(new NewChanelForSendFileMessage(userName));
        networkDownloadFile.sendMessage(new CommandMessage(CommandMessage.Command.DOWNLOAD, fileName, size));
        networkDownloadFile.changeHandlerForDownloadFile(userName, fileName, size);
    }

    public Map <String, Network> getListDownload() {
        return listDownload;
    }

    public Integer showMessageFileExist(String fileName) {
        Integer idx = null;
        int result = JOptionPane.showConfirmDialog(
                  MainWindow.this,
                  "Файл " + fileName +" уже существует на сервере, заменить?", "Внимание!",
                  JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {

            System.out.println("Выбрана замена файла rowcount " + table.getTable().getRowCount());
            for (Integer i =0; i<table.getTable().getRowCount(); i++) {
                String string = (String)table.getTable().getValueAt(i, 0);
                System.out.println(string+" " + string.length());
                if (fileName.equals(string)){
                    idx = i;
                    System.out.println("Совпадение номер строки " + i);
                    return idx;
                }else {
                    System.out.println("Совпадений нет");
                }
            }

        }else if (result == JOptionPane.NO_OPTION) {
            System.out.println("Выбрана отмена загрузки файла");
        }
        return idx;
    }


    public boolean showMessageForLocalStorage (String fileName) {
        boolean res = false;
        int result = JOptionPane.showConfirmDialog(
                  MainWindow.this,
                  "Файл " + fileName+" уже существует в локальном хранилище, заменить?", "Внимание!",
                  JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {

            System.out.println("Выбрана замена файла");
            res = true;
        }else if (result == JOptionPane.NO_OPTION) {
            System.out.println("Выбрана отмена загрузки файла");

        }

        return res;
    }


    public boolean showMessageForCloseWindow () throws InterruptedException {
        boolean res = false;
        int result = JOptionPane.showConfirmDialog(
                  MainWindow.this,
                  "Загрузка фала (-ов) не завершена. При выходе из программы загрузка будет остановлена, а файлы удалены (в перспективе!)", "Внимание!",
                  JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {
            for (Network net : listDownload.values()) {
                listForDeletFile.put(net.getFileData().getFileName(), net.getFileData().getStatus());
                net.stop();
                net.getThread().join();
            }

            if (listDownload.isEmpty()){

                for (Map.Entry st: listForDeletFile.entrySet()) {
                    if (st.getValue() == StatusFile.SEND) {
                        network.sendMessage(new CommandMessage(CommandMessage.Command.DELETE, st.getKey().toString(), null));
                    }else if (st.getValue() == StatusFile.DOWNLOAD) {
                        File fileExist = new File (CLIENT_STORAGE + userName + "/"+st.getKey().toString());
                        if (fileExist.exists()) {
                            if (fileExist.delete()) {
                                System.out.println("File delete on client");
                            }
                        }else {
                            System.out.println("file dont exist");
                        }
                    }
                }
            }
            System.out.println("Выход из программы и удаление файлов");
            res = true;
        }else if (result == JOptionPane.NO_OPTION) {
            System.out.println("Отмена выхода, продолжение загрузки");

        }
        return res;
    }

    public void showMessageDownloadComplited (String fileName) {

        JOptionPane.showMessageDialog(MainWindow.this,
                  "Скачивание файла "+fileName+" завершено",
                  "Скачивание завершено",
                  JOptionPane.PLAIN_MESSAGE);
    }

    public void showMessageDeleteDownloadFile(String fileName, Integer idx) throws InterruptedException {

        int result = JOptionPane.showConfirmDialog(
                  MainWindow.this,
                  "Вы действительно хотите остановить загрузку и удалить Файл " + fileName+"?", "Внимание!",
                  JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {
            listDownload.get(fileName).stop();
            listDownload.get(fileName).getThread().join();
            network.sendMessage(new CommandMessage(CommandMessage.Command.DELETE, fileName, idx));
        }else if (result == JOptionPane.NO_OPTION) {
            System.out.println("Выбрано продолжение загрузки файла");
        }
    }


    public int searchEqualsFileName (String fileName) {
        int idx = -1;
        for (int i =0; i<table.getTable().getRowCount(); i++) {
            String string = (String)table.getTable().getValueAt(i, 0);
            System.out.println(string+" " + string.length());
            if (fileName.equals(string)){
                idx = i;
                System.out.println("Совпадение номер строки " + i);
                break;
            }else {
                System.out.println("Совпадений нет");
            }
        }
        return idx;
    }
}
