package com.example.springboot.util;

import com.example.springboot.dto.user.UserCreateRequest;
import com.example.springboot.entity.enums.PatientStatus;
import com.example.springboot.entity.enums.PatientType;
import com.example.springboot.exception.BadRequestException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExcelUserParser {

    // 表头对应索引（根据Excel模板顺序）
    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int PASSWORD_INDEX = 2;
    private static final int ROLE_INDEX = 3;
    private static final int ID_CARD_INDEX = 4;
    private static final int PHONE_INDEX = 5;
    private static final int ALLERGY_INDEX = 6;
    private static final int MEDICAL_HISTORY_INDEX = 7;

    public List<UserCreateRequest> parseExcel(InputStream inputStream) throws IOException {
        List<UserCreateRequest> requests = new ArrayList<>();
        Workbook workbook = WorkbookFactory.create(inputStream);
        Sheet sheet = workbook.getSheetAt(0); // 读取第一个工作表

        // 从第二行开始读取（跳过表头）
        for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;

            UserCreateRequest request = new UserCreateRequest();
            try {
                // 解析必填字段
                String id = getCellValue(row.getCell(ID_INDEX));
                String name = getCellValue(row.getCell(NAME_INDEX));
                String password = getCellValue(row.getCell(PASSWORD_INDEX));
                String role = getCellValue(row.getCell(ROLE_INDEX));
                String idCard = getCellValue(row.getCell(ID_CARD_INDEX));
                String phone = getCellValue(row.getCell(PHONE_INDEX));

                // 验证必填字段
                validateRequiredField(id, "学号/工号", rowNum + 1);
                validateRequiredField(name, "姓名", rowNum + 1);
                validateRequiredField(password, "密码", rowNum + 1);
                validateRequiredField(role, "角色", rowNum + 1);
                validateRequiredField(idCard, "身份证号", rowNum + 1);
                validateRequiredField(phone, "手机号", rowNum + 1);
                validateRole(role, rowNum + 1);

                // 设置公共字段
                request.setId(id);
                request.setName(name);
                request.setPassword(password);
                request.setRole(role);
                request.setId_card(idCard);
                request.setPhone(phone);

                // 设置可选字段
                request.setAllergy_history(getCellValue(row.getCell(ALLERGY_INDEX)));
                request.setPast_medical_history(getCellValue(row.getCell(MEDICAL_HISTORY_INDEX)));

                // 设置默认状态（根据角色）
                if ("PATIENT".equals(role)) {
                    request.setPatientType(PatientType.student); // 默认学生类型
                    request.setPatientStatus(PatientStatus.active);
                }

                requests.add(request);
            } catch (Exception e) {
                throw new BadRequestException("第" + (rowNum + 1) + "行数据解析失败: " + e.getMessage());
            }
        }

        workbook.close();
        return requests;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return null;
        }
    }

    private void validateRequiredField(String value, String fieldName, int rowNum) {
        if (value == null || value.isEmpty()) {
            throw new BadRequestException(fieldName + "不能为空（第" + rowNum + "行）");
        }
    }

    private void validateRole(String role, int rowNum) {
        if (!"PATIENT".equals(role) && !"DOCTOR".equals(role) && !"ADMIN".equals(role)) {
            throw new BadRequestException("角色必须是PATIENT/DOCTOR/ADMIN（第" + rowNum + "行）");
        }
    }
}