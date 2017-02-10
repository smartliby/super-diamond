/**        
 * Copyright (c) 2013 by 苏州科大国创信息技术有限公司.    
 */    
package com.github.diamond.web.service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.druid.support.json.JSONUtils;

/**
 * Create on @2013-8-23 @上午10:26:17 
 * @author bsli@ustcinfo.com
 */
@Service
public class ConfigService {
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private ProjectService projectService;
	
	public List<Map<String, Object>> queryConfigs(Long projectId, Long moduleId, int offset, int limit) {
		String sql = "SELECT * FROM conf_project_config a, conf_project_module b "
				+ "WHERE a.module_id = b.module_id AND a.delete_flag =0 AND a.project_id=? ";
		
		if(moduleId != null) {
			sql = sql + " AND a.module_id = ? order by a.module_id limit ?,?";
			return jdbcTemplate.queryForList(sql, projectId, moduleId, offset, limit);
		} else {
			sql = sql + " order by a.module_id limit ?,?";
			return jdbcTemplate.queryForList(sql, projectId, offset, limit);
		}
		
	}
	
	public long queryConfigCount(Long projectId, Long moduleId) {
		String sql = "SELECT count(*) FROM conf_project_config a, conf_project_module b "
				+ "WHERE a.module_id = b.module_id AND a.delete_flag =0 AND a.project_id=? ";
		
		if(moduleId != null) {
			sql = sql + " AND a.module_id = ? order by a.module_id";
			return jdbcTemplate.queryForObject(sql, Long.class, projectId, moduleId);
		} else {
			sql = sql + " order by a.module_id";
			return jdbcTemplate.queryForObject(sql, Long.class, projectId);
		}
		
	}
	
	public String queryConfigs(String projectCode, String type, String format) {
		String sql = "SELECT * FROM conf_project_config a, conf_project_module b, conf_project c " +
				"WHERE a.module_id = b.module_id AND a.project_id=c.id AND a.delete_flag =0 AND c.proj_code=?";
		List<Map<String, Object>> configs = jdbcTemplate.queryForList(sql, projectCode);
		if("php".equals(format)) {
			return viewConfigPhp(configs, type);
		} else if("json".equals(format)) {
			return viewConfigJson(configs, type);
		} else
			return viewConfig(configs, type);
	}
	
	public String queryConfigs(String projectCode, String[] modules, String type, String format) {
		String sql = "SELECT * FROM conf_project_config a, conf_project_module b, conf_project c " +
				"WHERE a.module_id = b.module_id AND a.project_id=c.id AND a.delete_flag =0 AND c.proj_code=? "
				+ "AND b.module_name in ('" + StringUtils.join(modules, "','") + "')";
		
		List<Map<String, Object>> configs = jdbcTemplate.queryForList(sql, projectCode);
		if("php".equals(format)) {
			return viewConfigPhp(configs, type);
		} else if("json".equals(format)) {
			return viewConfigJson(configs, type);
		} else
			return viewConfig(configs, type);
	}
	
	public String queryValue(String projectCode, String module, String key, String type) {
		String sql = "SELECT * FROM conf_project_config a, conf_project_module b, conf_project c " +
				"WHERE a.module_id = b.module_id AND a.project_id=c.id AND a.delete_flag =0 AND c.proj_code=? "
				+ "AND b.module_name=? AND a.config_key=?";
		Map<String, Object> config = jdbcTemplate.queryForMap(sql, projectCode, module, key);
		if("development".equals(type)) {
			return (String)config.get("config_value");
		} else if("production".equals(type)) {
			return (String)config.get("production_value");
		} else if("test".equals(type)) {
			return (String)config.get("test_value");
		} else if("build".equals(type)) {
			return (String)config.get("build_value");
		} else
			return "";
	}
	
	@Transactional
	public void insertConfig(String configKey, String configValue, String configDesc, Long projectId, Long moduleId, String user) {
        String sql = "SELECT MAX(config_id)+1 FROM conf_project_config";
        long id = 1;
		try {
			id = jdbcTemplate.queryForObject(sql, Long.class);
		} catch(NullPointerException e) {
			;
		}

        sql = "INSERT INTO conf_project_config(config_id,config_key,config_value,config_desc,project_id,module_id,delete_flag,opt_user,opt_time," +
				"production_value,production_user,production_time,test_value,test_user,test_time,build_value,build_user,build_time) "
				+ "VALUES (?,?,?,?,?,?,0,?,?,?,?,?,?,?,?,?,?,?)";
		Date time = new Date();
		jdbcTemplate.update(sql, id, configKey, configValue, configDesc, projectId, moduleId, user, time,
				configValue, user, time, configValue, user, time, configValue, user, time);
		
		projectService.updateVersion(projectId);
	}
	
	@Transactional
	public void updateConfig(String type, Long configId, String configKey, String configValue, String configDesc, Long projectId, Long moduleId, String user) {
		if("development".equals(type)) {
			String sql = "update conf_project_config set config_key=?,config_value=?,config_desc=?,project_id=?,module_id=?,opt_user=?,opt_time=? where config_id=?";
			jdbcTemplate.update(sql, configKey, configValue, configDesc, projectId, moduleId, user, new Date(), configId);
			projectService.updateVersion(projectId, type);
		} else if("production".equals(type)) {
			String sql = "update conf_project_config set config_key=?,production_value=?,config_desc=?,project_id=?,module_id=?,production_user=?,production_time=? where config_id=?";
			jdbcTemplate.update(sql, configKey, configValue, configDesc, projectId, moduleId, user, new Date(), configId);
			projectService.updateVersion(projectId, type);
		} else if("test".equals(type)) {
			String sql = "update conf_project_config set config_key=?,test_value=?,config_desc=?,project_id=?,module_id=?,test_user=?,test_time=? where config_id=?";
			jdbcTemplate.update(sql, configKey, configValue, configDesc, projectId, moduleId, user, new Date(), configId);
			projectService.updateVersion(projectId, type);
		} else if("build".equals(type)) {
			String sql = "update conf_project_config set config_key=?,build_value=?,config_desc=?,project_id=?,module_id=?,build_user=?,build_time=? where config_id=?";
			jdbcTemplate.update(sql, configKey, configValue, configDesc, projectId, moduleId, user, new Date(), configId);
			projectService.updateVersion(projectId, type);
		}
	}
	
	public void deleteConfig(Long id, Long projectId) {
		String sql = "update conf_project_config set delete_flag=1 where config_id=?";
		jdbcTemplate.update(sql, id);
		projectService.updateVersion(projectId);
	}
	
	private String viewConfig(List<Map<String, Object>> configs, String type) {
		String message = "";
		
		boolean versionFlag = true;
		for(Map<String, Object> map : configs) {
			if(versionFlag) {
				if("development".equals(type)) {
					message += "#version = " + map.get("development_version") + "\r\n";
				} else if("production".equals(type)) {
					message += "#version = " + map.get("production_version") + "\r\n";
				} else if("test".equals(type)) {
					message += "#version = " + map.get("test_version") + "\r\n";
				} else if("build".equals(type)) {
					message += "#version = " + map.get("build_version") + "\r\n";
				}
				
				versionFlag = false;
			}
			
			String desc = (String)map.get("config_desc");
			desc = desc.replaceAll("\r\n", " ");
			if(StringUtils.isNotBlank(desc))
				message += "#" + desc + "\r\n";
			
			if("development".equals(type)) {
				message += map.get("config_key") + " = " + map.get("config_value") + "\r\n";
			} else if("production".equals(type)) {
				message += map.get("config_key") + " = " + map.get("production_value") + "\r\n";
			} else if("test".equals(type)) {
				message += map.get("config_key") + " = " + map.get("test_value") + "\r\n";
			} else if("build".equals(type)) {
				message += map.get("config_key") + " = " + map.get("build_value") + "\r\n";
			}
		}
		
		return message;
	}
	
	private String viewConfigPhp(List<Map<String, Object>> configs, String type) {
		String message = "<?php\r\n"
						+ "return array(\r\n"
						+ "\t//profile = " + type + "\r\n";
		
		boolean versionFlag = true;
		for(Map<String, Object> map : configs) {
			if(versionFlag) {
				if("development".equals(type)) {
					message += "\t//version = " + map.get("development_version") + "\r\n";
				} else if("production".equals(type)) {
					message += "\t//version = " + map.get("production_version") + "\r\n";
				} else if("test".equals(type)) {
					message += "\t//version = " + map.get("test_version") + "\r\n";
				} else if("build".equals(type)) {
					message += "\t//version = " + map.get("build_value") + "\r\n";
				}
				
				versionFlag = false;
			}
			
			String desc = (String)map.get("config_desc");
			if(StringUtils.isNotBlank(desc))
				message += "\t//" + desc + "\r\n";
			
			if("development".equals(type)) {
				message += "\t'" + map.get("config_key") + "' => " + convertType(map.get("config_value"));
			} else if("production".equals(type)) {
				message += "\t'" + map.get("config_key") + "' => " + convertType(map.get("production_value"));
			} else if("test".equals(type)) {
				message += "\t'" + map.get("config_key") + "' => " + convertType(map.get("test_value"));
			} else if("build".equals(type)) {
				message += "\t'" + map.get("config_key") + "' => " + convertType(map.get("build_value"));
			}
		}

		message += ");\r\n";
		
		return message;
	}
	
	private String viewConfigJson(List<Map<String, Object>> configs, String type) {
		Map<String, Object> confMap = new LinkedHashMap<String, Object>();
		boolean versionFlag = true;
		for(Map<String, Object> map : configs) {
			if(versionFlag) {
				if("development".equals(type)) {
					confMap.put("version", map.get("development_version"));
				} else if("production".equals(type)) {
					confMap.put("version", map.get("production_version"));
				} else if("test".equals(type)) {
					confMap.put("version", map.get("test_version"));
				} else if("build".equals(type)) {
					confMap.put("version", map.get("build_value"));
				}
				
				versionFlag = false;
			}
			
			if("development".equals(type)) {
				confMap.put(map.get("config_key").toString(), map.get("config_value"));
			} else if("production".equals(type)) {
				confMap.put(map.get("config_key").toString(), map.get("production_value"));
			} else if("test".equals(type)) {
				confMap.put(map.get("config_key").toString(), map.get("test_value"));
			} else if("build".equals(type)) {
				confMap.put(map.get("config_key").toString(), map.get("build_value"));
			}
		}
		
		return JSONUtils.toJSONString(confMap);
	}
	
	private String convertType(Object value) {
		String conf = String.valueOf(value).trim();
		if("true".equals(conf) || "false".equals(conf)) {
			return  conf + ",\r\n";
		} else if(isNumeric(conf)) {
			return  conf + ",\r\n";
		}else  {
			return  "'" + conf + "',\r\n";
		}
	}
	
	public final static boolean isNumeric(String s) {
		if (s != null && !"".equals(s.trim()))
			return s.matches("^[0-9]*$");
		else
			return false;
	}
}
