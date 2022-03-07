package ru.ifmo.dolzhanskii.sd.hw9.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Value;

@Getter
@Value
public class MasterResponse {

    Map<String, List<String>> response = new HashMap<>();
}
