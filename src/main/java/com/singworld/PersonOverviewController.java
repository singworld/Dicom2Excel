package com.singworld;

import com.alibaba.excel.EasyExcel;
import com.sun.xml.fastinfoset.stax.events.Util;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.tool.common.DicomFiles;
import org.dcm4che3.util.SafeClose;

import java.io.File;
import java.util.*;

public class PersonOverviewController {

    @FXML
    private Label outpath;
    @FXML
    private ProgressIndicator progressBar;//进度条组件
    @FXML
    private Label label;//文本组件

    @FXML
    private TextArea inpathlist;

    @FXML
    private Button runexcel;

    // Reference to the main application.
    private MainApp mainApp;

    private List<String> argList = new ArrayList<>();

    private int count;



    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    private void initialize() {

    }

    /**
     * Is called by the main application to give a reference back to itself.
     * 
     * @param mainApp
     */
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;

        // Add observable list data to the table
//        personTable.setItems(mainApp.getPersonData());
    }

    public void openFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Dicom Files", "*.dcm"), new FileChooser.ExtensionFilter("All Files", "*.*"));
        List<File> files = fileChooser.showOpenMultipleDialog(mainApp.getPrimaryStage());
        count = files.size();
        argList.clear();
        inpathlist.clear();
        if (count>0) {
            for (File file : files) {
                argList.add(file.getAbsolutePath());
                inpathlist.appendText(file.getAbsolutePath() +"\n");
            }
        }
    }

    public void openFolder(ActionEvent actionEvent) {

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose Folder");
        File directory = directoryChooser.showDialog(new Stage());
        argList.clear();
        if (directory != null) {

            if (directory.isDirectory()) {
                count = directory.list().length;
            }
            argList.add(directory.getAbsolutePath());
            inpathlist.setText(directory.getAbsolutePath());
        }


    }



    public void openOut(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose Folder");
        File directory = directoryChooser.showDialog(new Stage());
        if (directory != null) {
            outpath.setText(directory.getAbsolutePath());
        }
    }



    public void runexcel(ActionEvent actionEvent) {
        String text = inpathlist.getText();
        String text1 = outpath.getText();
        if(Util.isEmptyString(text) || Util.isEmptyString(text1)){
            label.setText("请选择输入输出路径后再执行");
            label.setTextFill(Color.RED);
            return;
        }

        runexcel.setDisable(true);
        runTask(new MyTask());

    }



    private void runTask(Task task) {
        progressBar.setVisible(true);
        progressBar.setDisable(false);
        label.setText("正在准备任务。。。");
        Service<Integer> service = new Service<Integer>() {
            @Override
            protected Task<Integer> createTask() {
                return task;
            }
        };
        //最主要的是这一步
        progressBar.progressProperty().bind(task.progressProperty());
        //使用service.getxxx()和使用回调函数中的newValue获取到的值是一样的
        task.messageProperty().addListener((observable, oldValue, newValue) -> {
            label.setText(service.getMessage());
        });
        task.valueProperty().addListener((observable, oldValue, newValue) -> {
            label.setTextFill(Color.GRAY);
            if ((int)newValue == -2){
                label.setTextFill(Color.GREEN);
                taskComplete();
            }
            if ((int) newValue == -1){
                label.setTextFill(Color.RED);
                taskOnError();
            }
        });
        service.restart();
    }

    /**
     * 执行任务出错
     */
    private void taskOnError() {

    }

    /**
     * 完成
     */
    private void taskComplete() {
        progressBar.setDisable(true);
        progressBar.setVisible(false);
    }

    private class MyTask extends Task{
        @Override
        protected Object call() throws Exception {
            //更新任务信息
            updateMessage("任务开始...");
            //更新任务返回值
            updateValue(0);
            try {
                List<DicomData> dicomMeta = getDicomMeta();
                String path = writeExcel(dicomMeta);
                updateMessage("导出成功！文件路径"+path);
                return -2;
            } catch (Exception e) {
                updateMessage("任务出错！");
                updateValue(-1);
                updateProgress(-1, 1);
                return -1;
            }

        }


        private List<DicomData> getDicomMeta(){
            Map<String, String> map = new HashMap<>();
            List<DicomData> list = new ArrayList<>();

            DicomFiles.scan(argList, true, new DicomFiles.Callback() {
                int i = 0;
                @Override
                public boolean dicomFile(File file, Attributes attributes, long l, Attributes attributes1) throws Exception {
                    i++;
                    Attributes attr = null;
                    updateProgress(i, count);
                    updateMessage("任务正在执行中" + i + " / "+count);
                    updateValue(i);

                    DicomInputStream in = new DicomInputStream(file);
                    try {
                        in.setIncludeBulkData(DicomInputStream.IncludeBulkData.NO);
                        attr = in.readDatasetUntilPixelData();

                        //id
                        String patientID = attr.getString(Tag.PatientID);

                        if (map.containsKey(patientID)){
                            return false;
                        }else {
                            map.put(patientID,patientID);
                        }

                        //姓名
                        String patientName = attr.getString(Tag.PatientName);
                        //生日
                        Date patientBirthDate = attr.getDate(Tag.PatientBirthDate);
                        //性别
                        String patientSex = attr.getString(Tag.PatientSex);
                        //年龄
                        String patientAge = attr.getString(Tag.PatientAge);
                        //扫描部位
                        String bodyPartExamined = attr.getString(Tag.BodyPartExamined);
                        //检查设备
                        //  String modality = attr.getString(Tag.Modality);
                        //检查号
                        String studyID = attr.getString(Tag.StudyID);
                        //机构名
                        String institutionName = attr.getString(Tag.InstitutionName);

                        DicomData dicomData = new DicomData();
                        dicomData.setPatientID(patientID);
                        dicomData.setPatientName(patientName);
                        dicomData.setPatientSex(patientSex);
                        dicomData.setPatientAge(patientAge);
                        dicomData.setPatientBirthDate(patientBirthDate);
                        dicomData.setStudyID(studyID);
                        dicomData.setInstitutionName(institutionName);
                        list.add(dicomData);

                    } finally {
                        SafeClose.close(in);
                    }
                    return true;
                }
            });

            return list;
        }

    }




    private String writeExcel(List<DicomData> list){
        String outpathText = outpath.getText();
        String fileName = outpathText+ "\\dicom信息" + System.currentTimeMillis() + ".xlsx";
        try {
            // 这里 需要指定写用哪个class去写，然后写到第一个sheet，名字为模板 然后文件流会自动关闭
            // 如果这里想使用03 则 传入excelType参数即可
            EasyExcel.write(fileName, DicomData.class).sheet("dicom").doWrite(list);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            runexcel.setDisable(false);
        }
        return fileName;
    }

}