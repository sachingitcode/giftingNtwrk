package com.gl.ceir.config.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gl.ceir.config.model.Script;

public interface SmsScriptRepository extends JpaRepository<Script, Long> {

}
