package com.singworld;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 基础数据类
 *
 * @author Jiaju Zhuang
 **/

//map.put("patientID",patientID);
//        map.put("patientName",patientName);
//        map.put("date",date);
//        map.put("patientSex",patientSex);
//        map.put("patientAge",patientAge);
//        map.put("bodyPartExamined",bodyPartExamined);
////                            map.put("modality",modality);
//        map.put("studyID",studyID);
//        map.put("institutionName",institutionName);
@Getter
@Setter
@EqualsAndHashCode
public class DicomData {
    @ExcelProperty("病人ID")
    private String patientID;
    @ExcelProperty("病人姓名")
    private String patientName;
    @ExcelProperty("病人性别")
    private String patientSex;
    @ExcelProperty("病人年龄")
    private String patientAge;
    @ExcelProperty("出生日期")
    private Date patientBirthDate;
    @ExcelProperty("检查号")
    private String studyID;
    @ExcelProperty("机构名称")
    private String institutionName;
}
