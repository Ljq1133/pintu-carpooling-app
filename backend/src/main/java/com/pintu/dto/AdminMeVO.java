package com.pintu.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminMeVO {

    private String id;
    private String username;
    private String name;
    private String role;
    private List<String> subRoles = new ArrayList<>();
    private List<String> subRoleLabels = new ArrayList<>();
    private List<AdminPermissionVO> permissions = new ArrayList<>();
}
