package com.github.diamond.web.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.diamond.web.model.Project;
import com.github.diamond.web.model.User;

/**
 * Create on @2013-7-18 @下午10:51:27 
 * @author melin
 */
@Service
public class ProjectService {
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private ModuleService moduleService;
	
	@Autowired
	private ConfigService configService;
	
	public List<Project> queryProjects(User user, int offset, int limit) {
		String sql = "SELECT b.id, b.proj_code, b.proj_name, a.user_name, b.owner_id FROM conf_user a, conf_project b " +
				"WHERE a.id=b.owner_id and b.delete_flag = 0 ";
		
		if(!"admin".equals(user.getUserCode())) {
			sql = sql + " AND b.owner_id = ? ORDER BY b.id asc limit ?,?";
		    return jdbcTemplate.query(sql, new ProjectRowMapper(), user.getId(), offset, limit);
		} else
			sql = sql + " ORDER BY b.id asc limit ?,?";
			return jdbcTemplate.query(sql, new ProjectRowMapper(), offset, limit);
	}
	
	public Long queryProjectCount(User user) {
		String sql = "SELECT count(*) FROM conf_user a, conf_project b " +
				"WHERE a.id=b.owner_id AND b.delete_flag = 0 ";
		
		if(!"admin".equals(user.getUserCode())) {
			sql = sql + " AND b.owner_id = ?";
		    return jdbcTemplate.queryForObject(sql, Long.class, user.getId());
		} else
			return jdbcTemplate.queryForObject(sql, Long.class);
	}
	
	public long findUserId(String userCode) {
		try {
			String sql = "SELECT ID FROM conf_user WHERE user_code = ?";
			long userid = jdbcTemplate.queryForObject(sql, new Object[]{userCode}, Long.class);
			return userid;
		} catch(DataAccessException e) {
			return 0;
		}
	}
	
	/**
	 * 检查项目是否存在
	 * 
	 * @param code
	 * @return
	 */
	public boolean checkProjectExist(String code) {
		String sql = "SELECT COUNT(*) FROM conf_project WHERE proj_code=?";
		int count = jdbcTemplate.queryForObject(sql, Integer.class, code);
		if(count == 1)
			return true;
					
		return false;
	}
	
	@Transactional
	public void saveProject(Project project, String copyCode, User user) {
		String sql = "SELECT MAX(id)+1 FROM conf_project";
		long projId = 1;
		try {
			projId = jdbcTemplate.queryForObject(sql, Long.class);
		} catch(NullPointerException e) {
			;
		}
		sql = "insert into conf_project (id, proj_code, proj_name, owner_id, create_time) values (?, ?, ?, ?, ?)";
		
		jdbcTemplate.update(sql, projId, project.getCode(), project.getName(), project.getOwnerId(), new Date());
		this.saveUser(projId, project.getOwnerId(), "development", "test", "build", "production", "admin");
		
		if(StringUtils.isNotBlank(copyCode)) {
			copyProjConfig(projId, copyCode, user.getUserCode());
		}
	}
	
	@Transactional
	public void deleteProject(long id) {
		String sql = "update conf_project set delete_flag = 1 where id = ?";
		jdbcTemplate.update(sql, id);
	}
	
	public List<User> queryUsers(long projectId, int offset, int limit) {
		String sql = "SELECT a.id, a.user_code, a.user_name FROM conf_user a WHERE a.id NOT IN " +
				"(SELECT b.user_id FROM conf_project_user b WHERE b.proj_id=?) AND a.delete_flag=0 order by a.id limit ?,?";
		
		return jdbcTemplate.query(sql, new UserRowMapper(), projectId, offset, limit);
	}
	
	public long queryUserCount(long projectId) {
		String sql = "SELECT count(*) FROM conf_user a WHERE a.id NOT IN " +
				"(SELECT b.user_id FROM conf_project_user b WHERE b.proj_id=?) AND a.delete_flag=0";
		
		return jdbcTemplate.queryForObject(sql, Long.class, projectId);
	}
	
	public List<User> queryProjUsers(long projectId) {
		String sql = "SELECT a.id, a.user_code, a.user_name FROM conf_user a WHERE a.id IN " +
				"(SELECT b.user_id FROM conf_project_user b WHERE b.proj_id=?) AND a.delete_flag=0";
		
		List<User> users = jdbcTemplate.query(sql, new UserRowMapper(), projectId);
		
		for(User user : users) {
			sql = "SELECT c.role_code FROM conf_project_user_role c WHERE c.proj_id = ?  AND c.user_id = ?";
			List<String> roles = jdbcTemplate.queryForList(sql, String.class, projectId, user.getId());
			user.setRoles(roles);
		}
		
		return users;
	}
	
	public List<String> queryRoles(long projectId, long userId) {
		String sql = "SELECT a.role_code FROM conf_project_user_role a WHERE a.proj_id=? AND a.user_id=? ORDER BY a.role_code";
		return jdbcTemplate.queryForList(sql, String.class, projectId, userId);
	}
	
	@Transactional
	public void saveUser(long projectId, long userId, String development, String test, String build, String production, String admin) {
		String sql = "insert into conf_project_user (proj_id, user_id) values (?, ?)";
		jdbcTemplate.update(sql, projectId, userId);
		
		sql = "insert into conf_project_user_role (proj_id, user_id, role_code) values (?, ?, ?)";
		if(StringUtils.isNotBlank(admin)) {
			jdbcTemplate.update(sql, projectId, userId, "admin");
			//如果拥有admin权限，自动添加development、test、build、production
			development = "development";
			test = "test";
			build = "build";
			production = "production";
		}
		if(StringUtils.isNotBlank(development)) {
			jdbcTemplate.update(sql, projectId, userId, "development");
		}
		if(StringUtils.isNotBlank(test)) {
			jdbcTemplate.update(sql, projectId, userId, "test");
		}
		if(StringUtils.isNotBlank(build)) {
			jdbcTemplate.update(sql, projectId, userId, "build");
		}
		if(StringUtils.isNotBlank(production)) {
			jdbcTemplate.update(sql, projectId, userId, "production");
		}
	}
	
	@Transactional
	public void deleteUser(long projectId, long userId) {
		String sql = "delete from conf_project_user_role where proj_id = ? and user_id = ?";
		jdbcTemplate.update(sql, projectId, userId);
		sql = "delete from conf_project_user where proj_id = ? and user_id = ?";
		jdbcTemplate.update(sql, projectId, userId);
	}
	
	/**
	 * 查询用户所拥有的项目
	 * 
	 * @param userId
	 */
	public List<Project> queryProjectForUser(User user, int offset, int limit) {
		if("admin".equals(user.getUserCode())) {
			String sql = "SELECT distinct b.id, b.proj_code, b.proj_name FROM conf_project_user a, conf_project b " +
					"WHERE a.proj_id = b.id AND b.delete_flag = 0 order by b.ID desc limit ?, ?";
			List<Project> projects = jdbcTemplate.query(sql, new RowMapper<Project>() {
	
				public Project mapRow(ResultSet rs, int rowNum) throws SQLException,
						DataAccessException {
					Project project = new Project();
					project.setId(rs.getLong(1));
					project.setCode(rs.getString(2));
					project.setName(rs.getString(3));
					return project;
				}
			}, offset, limit);
			return projects;
		} else {
			String sql = "SELECT distinct b.id, b.proj_code, b.proj_name FROM conf_project_user a, conf_project b " +
					"WHERE a.proj_id = b.id and a.user_id=? AND b.delete_flag = 0 order by b.id desc limit ?, ?";
			List<Project> projects = jdbcTemplate.query(sql, new RowMapper<Project>() {
	
				public Project mapRow(ResultSet rs, int rowNum) throws SQLException,
						DataAccessException {
					Project project = new Project();
					project.setId(rs.getLong(1));
					project.setCode(rs.getString(2));
					project.setName(rs.getString(3));
					return project;
				}
			}, user.getId(), offset, limit);
			return projects;
		}
	}
	
	/**
	 * 查询用户所拥有的项目数量
	 * 
	 * @param user
	 */
	public long queryProjectCountForUser(User user) {
		if("admin".equals(user.getUserCode())) {
			String sql = "select count(*) from (SELECT distinct b.id FROM conf_project_user a, conf_project b " +
					"WHERE a.proj_id = b.id AND b.delete_flag = 0) as proj";
			return jdbcTemplate.queryForObject(sql, Long.class);
		} else {
			String sql = "select count(*) from (SELECT distinct b.id FROM conf_project_user a, conf_project b " +
					"WHERE a.proj_id = b.id and a.user_id=? AND b.delete_flag = 0) as proj";
			return jdbcTemplate.queryForObject(sql, Long.class, user.getId());
		}
	}
	
	/**
	 * 增加配置项时，增加版本号
	 * @param projectId
	 */
	@Transactional
	public void updateVersion(Long projectId) {
		String sql = "update conf_project set development_version=development_version+1,production_version=production_version+1," +
				"test_version=test_version+1 where id=?";
		jdbcTemplate.update(sql, projectId);
	}
	
	/**
	 * 增加配置项时，增加版本号
	 * @param projectId
	 */
	@Transactional
	public void updateVersion(Long projectId, String type) {
		if("development".equals(type)) {
			String sql = "update conf_project set development_version=development_version+1 where id=?";
			jdbcTemplate.update(sql, projectId);
		} else if("production".equals(type)) {
			String sql = "update conf_project set production_version=production_version+1 where id=?";
			jdbcTemplate.update(sql, projectId);
		} else if("test".equals(type)) {
			String sql = "update conf_project set test_version=test_version+1 where id=?";
			jdbcTemplate.update(sql, projectId);
		}
	}
	
	public 	Map<String, Object> queryProject(Long projectId) {
		String sql = "select * from conf_project where id=?";
		return jdbcTemplate.queryForMap(sql, projectId);
	}
	
	private void copyProjConfig(long projId, String projCode, String userCode) {
		String sql = "SELECT b.module_id, b.module_name FROM conf_project a, conf_project_module b "
				+ "WHERE a.id = b.proj_id AND a.proj_code = ?";
		List<Map<String, Object>> modules = jdbcTemplate.queryForList(sql, projCode);
		
		for(Map<String, Object> module : modules) {
			long moduleId = moduleService.save(projId, (String)module.get("module_name"));
			sql = "SELECT b.config_key, b.config_value, b.config_desc FROM conf_project a, conf_project_config b "
					+ "WHERE a.id = b.project_id AND a.proj_code=? AND b.module_id = ?";
			List<Map<String, Object>> configs = jdbcTemplate.queryForList(sql, projCode, module.get("module_id"));
			
			for(Map<String, Object> conf : configs) {
				configService.insertConfig((String)conf.get("config_key"), (String)conf.get("config_value"), (String)conf.get("config_desc"),
						projId, moduleId, userCode);
			}
		}
	}
	
	private class ProjectRowMapper implements RowMapper<Project> {

		public Project mapRow(ResultSet rs, int rowNum) throws SQLException,
				DataAccessException {
			Project project = new Project();
			project.setId(rs.getLong(1));
			project.setCode(rs.getString(2));
			project.setName(rs.getString(3));
			project.setUserName(rs.getString(4));
			project.setOwnerId(rs.getLong(5));
			return project;
		}
	}
	
	private class UserRowMapper implements RowMapper<User> {

		public User mapRow(ResultSet rs, int rowNum) throws SQLException,
				DataAccessException {
			User user = new User();
			user.setId(rs.getLong(1));
			user.setUserCode(rs.getString(2));
			user.setUserName(rs.getString(3));
			return user;
		}
	}
}
