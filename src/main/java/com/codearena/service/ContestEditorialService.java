package com.codearena.service;

import java.util.List;

import com.codearena.dto.ContestEditorialDto;
import com.codearena.entity.ContestEditorial;
import com.codearena.entity.User;

public interface ContestEditorialService {

    ContestEditorial createOrUpdateEditorial(Long contestId, Long problemId, String content, User author);

    List<ContestEditorialDto> getEditorials(Long contestId);

    ContestEditorialDto getEditorial(Long contestId, Long problemId);
}
