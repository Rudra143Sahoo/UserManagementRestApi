package com.nt.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import com.nt.bindings.ActivateUser;
import com.nt.bindings.LoginCredentials;
import com.nt.bindings.RecoverPassword;
import com.nt.bindings.UserAccount;
import com.nt.entity.UserMaster;
import com.nt.repository.IUserMasterRepository;
import com.nt.utils.EmailUtils;
@Service
public class UserMgmtServiceImpl implements IUserMgmtService {
	@Autowired
private IUserMasterRepository userMasterRepo;
	@Autowired
	private EmailUtils emailUtils;
	 @Autowired
	    private Environment env;
	@Override
	public String registerUser(UserAccount user)throws Exception {
		//convert UserAccount data to UserMaster obj data
		UserMaster master=new UserMaster();
		BeanUtils.copyProperties(user, master);
		//set random string of 6 char as password
		String tempPwd=generateRandomPassword(6);
		master.setPassword(tempPwd);
		master.setActive_sw("InActive");
		//save object
		UserMaster savedMaster=userMasterRepo.save(master);
		//perform send mail operation
		String subject="User Registration success";
		String body=readEmailMessageBody(env.getProperty("mailbody.registeruser.location"),user.getName(),tempPwd);
		emailUtils.sendEmailMessage(user.getEmail(),subject,body);
		return savedMaster!=null?"User is registered with id value::"+savedMaster.getUserId():"problem is user registration";
	}

	@Override
	public String activeUserAccount(ActivateUser user) {
		//use findBy method
		UserMaster entity= userMasterRepo.findByEmailAndPassword(user.getEmail(),user.getTempPassword());
		if(entity==null)
			return "User is not found for activation";
		else {
			//set password
			entity.setPassword(user.getConfirmPassword());
			//change the user account status to active
			entity.setActive_sw("Active");
			//update the object
			UserMaster updatedEntity=userMasterRepo.save(entity);
			return "User is activated with new password";
		}
		
		
}

	@Override
	public String login(LoginCredentials credentials) {
		//convert Logincredentials to UserMaster obj
		UserMaster master=new UserMaster();
		BeanUtils.copyProperties(credentials, master);
		//prepare example obj
		Example<UserMaster> example=Example.of(master);
		List<UserMaster> listEntities=userMasterRepo.findAll(example);
	if(listEntities.size()==0)
		return"Invalid Credentials";
	else {
		//get entity obj
		UserMaster entity=listEntities.get(0);
		if(entity.getActive_sw().equalsIgnoreCase("Active")) {
			return "valid credentials and login successfull";
		}
		else {
			return "UserAccount is not Active";
		}
	}
	
	
	}

	@Override
	public List<UserAccount> listUsers() {
		//Load all entities and convert to UserAccount obj
		List<UserAccount> listUsers=userMasterRepo.findAll().stream().map(entity->{
		                                                                           UserAccount user=new UserAccount();
		                                                                           BeanUtils.copyProperties(entity, user);
		                                                                           return user;
		}).toList();
				return listUsers;
	}

	@Override
	public UserAccount showUserByUserId(Integer id) {
		//Load the user by user id
		Optional<UserMaster> opt=userMasterRepo.findById(id);
		UserAccount account=null;
		if(opt.isPresent()) {
			account=new UserAccount();
			BeanUtils.copyProperties(opt.get(), account);
		}
		return account;
	}

	@Override
	public UserAccount showUserByEmailAndName(String email, String name) {
		//use the custom findBy() method
		UserMaster master =userMasterRepo.findByNameAndEmail(name,email);
		UserAccount account=null;
		if(master!=null) {
			account=new UserAccount();
			BeanUtils.copyProperties(master, account);
			
		}
		return account;
	}

	@Override
	public String updateUser(UserAccount user) {
		//use the custom findBy() method
		Optional<UserMaster> opt=userMasterRepo.findById(user.getUserId());
		if(opt.isPresent()) {
			//get entity object
			UserMaster master=opt.get();
			BeanUtils.copyProperties(user, master);
			userMasterRepo.save(master);
			return "user details are updated";
			
			

			}
		else {
			return "User not found for updation";
			
		}
		
	}

	@Override
	public String deleteUserById(Integer id) {
		//Load the obj
		Optional<UserMaster> opt=userMasterRepo.findById(id);
		if(opt.isPresent()) {
			userMasterRepo.deleteById(id);
			return "user is deleted";
		}
		return "user is not found for deletion";
	}

	@Override
	public String changeUserStatus(Integer id, String status) {
		//load the obj
		Optional<UserMaster> opt=userMasterRepo.findById(id);
		if(opt.isPresent()) {
			//get entity obj
			UserMaster master=opt.get();
			//change the status
			master.setActive_sw(status);
			//update the obj
			userMasterRepo.save(master);
			return "User status changed"; 
			
		}
		return "user not found for changing the status";
	}

	@Override
	public String recoverPassword(RecoverPassword recover)throws Exception {
		//get UserMaster by name,email
		UserMaster master=userMasterRepo.findByNameAndEmail(recover.getName(),recover.getEmail());
		if(master!=null) {
			String pwd=master.getPassword();
			//send the recover password to the email account
			String subject ="mail for password recovery";
			String mailBody=readEmailMessageBody(env.getProperty("mailbody.recoverpwd.location"),recover.getName(),pwd);
			emailUtils.sendEmailMessage(recover.getEmail(),subject,mailBody);
			return pwd;
		}
		return "user and email is not found";
	}
	private String generateRandomPassword(int length) {
		//a list of character to choose in form of string
		String AlphaNumericStr="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	//creating a stringbuilder size of AlphaNumericStr
		StringBuilder randomWord=new StringBuilder(length);
		int i;
		for(i=0;i<length;i++) {
			//generating a random no using math.random()
			
			int ch=(int)(AlphaNumericStr.length()*Math.random());
			//adding random char one by one at the end of random word
			randomWord.append(AlphaNumericStr.charAt(ch));
		}
	return randomWord.toString();
	}
	
	private String readEmailMessageBody(String fileName,String fullName,String pwd)throws Exception{
		String mailBody=null;
		String url="";
		try(FileReader reader=new FileReader(fileName);
				BufferedReader br=new BufferedReader(reader)){
			StringBuffer buffer=new StringBuffer();
			String line =null;
			do {
				line=br.readLine();
				buffer.append(line);
				
			}while(line!=null);
			mailBody=buffer.toString();
			mailBody.replace("{FULL NAME}",fullName);
			mailBody.replace("{PWD}",pwd);
			mailBody.replace("{URL}",url);
			
		}
		catch(Exception e) {
			e.printStackTrace();
			throw e;
		}
		return mailBody;		
	}

}
