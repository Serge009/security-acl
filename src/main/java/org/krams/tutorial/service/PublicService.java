package org.krams.tutorial.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.krams.tutorial.domain.Post;
import org.krams.tutorial.domain.PublicPost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for processing Public related posts. 
 * <p>
 * For a complete reference to Spring JDBC and JdbcTemplate
 * see http://static.springsource.org/spring/docs/3.0.x/spring-framework-reference/html/jdbc.html
 * <p>
 * For transactions, see http://static.springsource.org/spring/docs/3.0.x/spring-framework-reference/html/transaction.html
 */
@Service("publicService")
@Transactional
public class PublicService implements GenericService {

	protected static Logger logger = Logger.getLogger("service");

	@Autowired
	private MutableAclService mutableAclService;
	
	// We'll be calling SQL statements. SimpleJdbcTemplate is a perfect tool.
	private SimpleJdbcTemplate jdbcTemplate;
	
	@Resource(name="bulletinDataSource")
	public void setDataSource(DataSource dataSource) {
	    this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	public Post getSingle(Long id) {
		try {
			logger.debug("Retrieving single public post");
			
			// Prepare SQL statement
			String sql = "select id, date, message from public_post where id = ?";
			
			// Assign values to parameters
			Object[] parameters = new Object[] {id};
			
			// Map SQL result to a Java object
			RowMapper<Post> mapper = new RowMapper<Post>() {  
		        public Post mapRow(ResultSet rs, int rowNum) throws SQLException {
		        	Post post = new PublicPost();
		        	post.setId(rs.getLong("id"));
		        	post.setDate(rs.getDate("date"));
		        	post.setMessage(rs.getString("message"));
		            return post;
		        }
		    };
		    
		    // Run query then return result
		    return jdbcTemplate.queryForObject(sql, mapper, parameters);
		
		} catch (Exception e) {
			logger.error(e);
			return null;
		}	
	}
	
	public List<Post> getAll() {
		try {
			logger.debug("Retrieving all public posts");
			
			// Prepare SQL statement
			String sql = "select id, date, message from public_post";
			
			// Map SQL result to a Java object
			RowMapper<Post> mapper = new RowMapper<Post>() {  
		        public Post mapRow(ResultSet rs, int rowNum) throws SQLException {
		        	Post post = new PublicPost();
		        	post.setId(rs.getLong("id"));
		        	post.setDate(rs.getDate("date"));
		        	post.setMessage(rs.getString("message"));
		            return post;
		        }
		    };
		
		    // Run query then return result
		    return jdbcTemplate.query(sql, mapper);
		
		} catch (Exception e) {
			logger.error(e);
			return null;
		}	
	}
	
	public Boolean add(Post post)  {
		try {
			logger.debug("Adding new post");
	
			// Prepare SQL statement
			String sql = "insert into public_post(date, message) values " +
					"(:date, :message)";
			
			// Assign values to parameters
			Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put("date", post.getDate());
			parameters.put("message", post.getMessage());
			
			// Save
			int postId = jdbcTemplate.update(sql, parameters);

			addPermission(postId, new PrincipalSid("john"), BasePermission.ADMINISTRATION);
			// Return
			return true;
			
		} catch (Exception e) {
			logger.error(e);
			return false;
		}	
	}

	public void addPermission(Integer postId, Sid recipient, Permission permission) {
		MutableAcl acl;
		ObjectIdentity oid = new ObjectIdentityImpl(Post.class, postId);

		try {
			acl = (MutableAcl) mutableAclService.readAclById(oid);
		} catch (NotFoundException nfe) {
			acl = mutableAclService.createAcl(oid);
		}

		acl.insertAce(acl.getEntries().size(), permission, recipient, true);
		acl.setOwner(recipient);

		mutableAclService.updateAcl(acl);

		logger.debug("Added permission " + permission + " for Sid " + recipient + " contact " + postId);

//		acl.insertAce(0, BasePermission.ADMINISTRATION, new PrincipalSid(message.getAuthor()), true);
//		acl.insertAce(1, BasePermission.DELETE, new GrantedAuthoritySid("ROLE_ADMIN"), true);
//		acl.insertAce(2, BasePermission.READ, new GrantedAuthoritySid("ROLE_USER"), true);
//		mutableAclService.updateAcl(acl);
	}
	
	public Boolean edit(Post post)  {
		try {
			logger.debug("Adding new post");
	
			// Prepare our SQL statement
			String sql = "update public_post set date = :date, " +
					"message = :message where id = :id";
			
			// Assign values to parameters
			Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put("id", post.getId());
			parameters.put("date", post.getDate());
			parameters.put("message", post.getMessage());
			
			// Save
			jdbcTemplate.update(sql, parameters);
			
			// Return
			return true;
			
		} catch (Exception e) {
			logger.error(e);
			return false;
		}		
	}

	public Boolean delete(Post post)  {
		try {
			logger.debug("Deleting existing post");
			
			// Prepare our SQL statement
			String sql = "delete from public_post where id = ?";
			
			// Assign values to parameters
			Object[] parameters = new Object[] {post.getId()};
			
			// Delete
			jdbcTemplate.update(sql, parameters);
	
			// Return
			return true;
			
		} catch (Exception e) {
			logger.error(e);
			return false;
		}	
	}
}