package com.gl.ceir.config.service.impl;

import java.util.List;


import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gl.ceir.config.model.ImsieSeries;
import com.gl.ceir.config.repository.ImsieSeriesRepository;
import com.gl.ceir.config.service.ImsieSeriesService;

@Service
public class ImsieSeriesServiceImpl implements ImsieSeriesService {

	private static final Logger logger = Logger.getLogger(ImsieSeriesServiceImpl.class);

	@Autowired
	private ImsieSeriesRepository imsieSeriesRepository;

	@Override
	public List<ImsieSeries> getAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImsieSeries save(ImsieSeries t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImsieSeries get(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImsieSeries update(ImsieSeries t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(Long t) {
		// TODO Auto-generated method stub

	}

}
