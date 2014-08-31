package com.amcolabs.quizapp.appcontrollers;

import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.view.View.OnClickListener;

import com.amcolabs.quizapp.AppController;
import com.amcolabs.quizapp.QuizApp;
import com.amcolabs.quizapp.Screen;
import com.amcolabs.quizapp.User;
import com.amcolabs.quizapp.UserDeviceManager;
import com.amcolabs.quizapp.configuration.Config;
import com.amcolabs.quizapp.databaseutils.Category;
import com.amcolabs.quizapp.databaseutils.Quiz;
import com.amcolabs.quizapp.datalisteners.DataInputListener;
import com.amcolabs.quizapp.popups.StaticPopupDialogBoxes;
import com.amcolabs.quizapp.screens.QuestionScreen;
import com.amcolabs.quizapp.screens.QuizzesScreen;
import com.amcolabs.quizapp.screens.HomeScreen;
import com.amcolabs.quizapp.screens.UserProfileScreen;
import com.amcolabs.quizapp.screens.WelcomeScreen;
import com.amcolabs.quizapp.uiutils.UiUtils.UiText;
import com.androidsocialnetworks.lib.AccessToken;
import com.androidsocialnetworks.lib.SocialNetworkManager;
import com.androidsocialnetworks.lib.SocialNetworkManager.OnInitializationCompleteListener;
import com.androidsocialnetworks.lib.impl.FacebookSocialNetwork;
import com.androidsocialnetworks.lib.impl.GooglePlusSocialNetwork;
import com.androidsocialnetworks.lib.listener.OnLoginCompleteListener;
import com.androidsocialnetworks.lib.listener.OnRequestAccessTokenCompleteListener;
import com.androidsocialnetworks.lib.listener.OnRequestDetailedSocialPersonCompleteListener;
import com.androidsocialnetworks.lib.persons.FacebookPerson;
import com.androidsocialnetworks.lib.persons.GooglePlusPerson;
import com.androidsocialnetworks.lib.persons.SocialPerson;
import com.google.android.gms.plus.model.people.Person.Gender;


public class UserMainPageController  extends AppController implements OnInitializationCompleteListener, OnLoginCompleteListener, OnRequestDetailedSocialPersonCompleteListener<SocialPerson>{
	 
	public static final String SOCIAL_NETWORK_TAG = "com.amcolabs.quizapp.loginscreen";
    protected boolean mSocialNetworkManagerInitialized = false;
    User user= null;
	private SocialNetworkManager mSocialNetworkManager;
	public UserMainPageController(QuizApp quizApp) {
		super(quizApp);
	}

//	public WelcomeScreen welcomeScreen;
//	public SignUpScreen signUpScreen;
//	public HomeScreen categoriesScreen;
//	public TopicsScreen topicsScreen;
	 
	public void checkAndShowCategories(){
		String encodedKey = quizApp.getUserDeviceManager().getPreference(Config.PREF_ENCODED_KEY, null);
		if(encodedKey==null){
			//on fetch update new categories and draw the categories
			quizApp.getServerCalls().getAllUpdates(new DataInputListener<Boolean>(){
				@Override
				public String onData(Boolean s) {
					insertScreen(new UserProfileScreen(UserMainPageController.this));
					if(s){
						showUserHomeScreen();
					}
					else{
						StaticPopupDialogBoxes.alertPrompt(quizApp.getFragmentManager(), UiText.COULD_NOT_CONNECT.getValue(), null);
					}
					return super.onData(s);
				}
			});
		}
		else if(quizApp.getUserDeviceManager().getPreference(Config.PREF_NOT_ACTIVATED, null)!=null){
			checkAndShowVerificationScreen();
		}
		else{
			showWelcomeScreen();
		}
	}
	
	private void checkAndShowVerificationScreen() {
		quizApp.getServerCalls().checkVerificationStatus(new DataInputListener<String>(){
			@Override
			public String onData(String encodedKey) {
				if(encodedKey!=null){
					showUserHomeScreen();
				}
				return null;
			}
		});// on ACTIVATED, save user quizApp setUser
	}
	
	public void onCategorySelected(Category category){
		clearScreen();
		QuizzesScreen categoryQuizzesScreen = new QuizzesScreen(this);
		List<Quiz> quizzes = category.getQuizzes(quizApp);
		categoryQuizzesScreen.addQuizzesToList(category.description , quizzes, new DataInputListener<Quiz>(){
			@Override
			public String onData(Quiz s) {
				onQuizSelected(s);
				return super.onData(s);
			}
		});
		insertScreen(categoryQuizzesScreen);
	}
	
	public void onQuizSelected(Quiz quiz){
		clearScreen();
		ProgressiveQuizController progressiveQuiz = (ProgressiveQuizController) quizApp.loadAppController(ProgressiveQuizController.class);
		progressiveQuiz.initlializeQuiz(null);
	}
	
	private void showUserHomeScreen() {
		clearScreen();
		HomeScreen cs= new HomeScreen(this);
//		ArrayList<Category> categories = new ArrayList<Category>();
//		for(int i=0;i<10;i++){
//			categories.add(Category.createDummy());
//		}
		List<Category> categories = quizApp.getDataBaseHelper().getCategories(5);
		cs.addCategoriesView(categories, categories.size()>4);
		 
		List<Quiz> quizzes = quizApp.getDataBaseHelper().getAllQuizzes(5);
		cs.addUserQuizzesView(quizzes ,quizzes.size()>4);
		
//		cs.addFeedView();
//		cs.addQuizzes();
		
		insertScreen(cs);
	}
	

	@Override
	public void beforeScreenRemove(Screen screen) {
		if(screen instanceof WelcomeScreen){
			onRemoveWelcomeScreen();
		}
	}
	
    public void onRemoveWelcomeScreen() {//destroy msocialNetwork
    	if(mSocialNetworkManager.getSocialNetwork(GooglePlusSocialNetwork.ID).isConnected()){
    		mSocialNetworkManager.getGooglePlusSocialNetwork().cancelAll();
    	}
    	if(mSocialNetworkManager.getSocialNetwork(FacebookSocialNetwork.ID).isConnected()){
    		mSocialNetworkManager.getFacebookSocialNetwork().cancelAll();
    	}
    	
    }
	
	public WelcomeScreen showWelcomeScreen(){
		  WelcomeScreen welcomeScreen = new WelcomeScreen(this);
	      mSocialNetworkManager = (SocialNetworkManager) quizApp.getFragmentManager().findFragmentByTag(SOCIAL_NETWORK_TAG);
		  if (mSocialNetworkManager == null) {
			    mSocialNetworkManager = SocialNetworkManager.Builder.from(quizApp.getActivity())
			            .facebook(new ArrayList<String>())
			            .googlePlus()
			            .build();
			    quizApp.getFragmentManager().beginTransaction().add(mSocialNetworkManager, SOCIAL_NETWORK_TAG).commit();
			    mSocialNetworkManager.setOnInitializationCompleteListener(this);
			} else {
			    mSocialNetworkManagerInitialized = true;
			}

		welcomeScreen.getPlusButton().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				GooglePlusSocialNetwork plusNetwork = (GooglePlusSocialNetwork) mSocialNetworkManager.getSocialNetwork(GooglePlusSocialNetwork.ID);
				if(!plusNetwork.isConnected()){
					plusNetwork.requestLogin(UserMainPageController.this);
				}
				else{
					afterGooglePlusConnected();
				}
			}
		});
		welcomeScreen.getFacebookButton().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				FacebookSocialNetwork fbNetwork = (FacebookSocialNetwork) mSocialNetworkManager.getSocialNetwork(FacebookSocialNetwork.ID);
				if(!fbNetwork.isConnected()){
					fbNetwork.requestLogin(UserMainPageController.this);
				}
				else{
					afterFbConnected();
				}
			}
		});
		
        insertScreen(welcomeScreen);
        return welcomeScreen;
	}

	protected void onFacebookButtonPressed() {
 
    }

  
	public void afterUserLoggedIn(User user){
		quizApp.setUser(user);
		checkAndShowCategories();
	}

	@Override
	public void onDestroy() {
		Screen screen = quizApp.peekCurrentScreen();
		while(screen!=null){
			if(screen instanceof WelcomeScreen){
				onRemoveWelcomeScreen();
			}
		}
	}
	
	public void afterGooglePlusConnected(){
		final GooglePlusSocialNetwork plusNetwork = (GooglePlusSocialNetwork) mSocialNetworkManager.getSocialNetwork(GooglePlusSocialNetwork.ID);
		plusNetwork.requestAccessToken(quizApp.getActivity(), new OnRequestAccessTokenCompleteListener() {
			@Override
			public void onError(int socialNetworkID, String requestID,
					String errorMessage, Object data) {
				System.out.println("");
			}
			@Override
			public void onRequestAccessTokenComplete(int socialNetworkID,AccessToken accessToken) {
				user.googlePlus = accessToken.token;
				plusNetwork.requestDetailedCurrentPerson(UserMainPageController.this);
			}
		});
	}
	public void afterFbConnected(){
		
	}

	@Override
	public void onSocialNetworkManagerInitialized() {
		if(mSocialNetworkManagerInitialized) return;
	    mSocialNetworkManager.getSocialNetwork(GooglePlusSocialNetwork.ID).isConnecting();
		mSocialNetworkManagerInitialized = true;
		GooglePlusSocialNetwork plusNetwork = (GooglePlusSocialNetwork) mSocialNetworkManager.getSocialNetwork(GooglePlusSocialNetwork.ID);
		FacebookSocialNetwork fbNetwork = (FacebookSocialNetwork) mSocialNetworkManager.getSocialNetwork(FacebookSocialNetwork.ID);
		user = new User();
		if(plusNetwork.isConnected() && !plusNetwork.isConnecting()){
			afterGooglePlusConnected();
		}
		if(plusNetwork.isConnected() && plusNetwork.isConnecting()){
			plusNetwork.requestLogin(UserMainPageController.this);
		}
		
		//else wait for user to click
		if(fbNetwork.isConnected()){
			
		}
	}
	@Override
	public void onRequestDetailedSocialPersonSuccess(int socialNetworkID,SocialPerson socialPerson) {		
		String details = null;
		switch(socialNetworkID){
			case GooglePlusSocialNetwork.ID:
				GooglePlusPerson gPerson;
				gPerson =((GooglePlusPerson)socialPerson);
				details = gPerson.toString();
				user.uid = gPerson.id;
				user.deviceId = UserDeviceManager.getDeviceId(quizApp.getContentResolver());
				user.name = gPerson.name;
				user.emailId = gPerson.email;
				user.pictureUrl = gPerson.avatarURL;
				user.coverUrl = gPerson.coverURL;
				user.place = gPerson.currentLocation;
				user.gender = gPerson.gender==Gender.MALE ?"male":"female";
				user.birthday = 0;//gPerson.birthday;
				break;
			case FacebookSocialNetwork.ID:
				FacebookPerson fPerson = (FacebookPerson)socialPerson;
				user.uid = fPerson.id;
				user.deviceId = UserDeviceManager.getDeviceId(quizApp.getContentResolver());
				user.name = fPerson.name;
				user.emailId = fPerson.email;
				user.pictureUrl = fPerson.avatarURL;
				user.coverUrl = fPerson.coverUrl;
				user.place = fPerson.city;
				user.gender = fPerson.gender;
				user.birthday = 0;//fPerson.birthday;
				break;
	}
		DataInputListener<User> loginListener = new DataInputListener<User>(){
			@Override
			public String onData(User s) {
				UserMainPageController.this.afterUserLoggedIn(s);
				return super.onData(s);
			}
		};
		
		switch(socialNetworkID){
			case GooglePlusSocialNetwork.ID:
				quizApp.getServerCalls().doGooglePlusLogin(user,loginListener);
				break;
			case FacebookSocialNetwork.ID:
				quizApp.getServerCalls().doFacebookLogin(user,loginListener); 
				break;
		}
	}
	
	@Override
	public void onLoginSuccess(int socialNetworkID) {
		if(socialNetworkID == GooglePlusSocialNetwork.ID){
			afterGooglePlusConnected();
		}
		if(socialNetworkID == FacebookSocialNetwork.ID){
			afterFbConnected();
		}
	}

	@Override
	public void onError(int socialNetworkID, String requestID,String errorMessage, Object data) {
		StaticPopupDialogBoxes.alertPrompt(quizApp.getFragmentManager(), requestID+errorMessage, null);
	}
	
	
	public void showAllCategories() {
		
	}

	public void showAllUserQuizzes() {
		
	}
}
