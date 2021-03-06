package br.edu.ufcg.ccc.projeto2.warofkingdoms.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.entities.Action;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.entities.Conflict;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.entities.House;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.entities.Move;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.entities.Player;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.entities.Territory;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.management.CommunicationManager;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.management.GameManager;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.management.HouseTokenManager;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.management.NetworkManager;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.management.ProfileManager;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.management.TerritoryUIManager;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.networking.ConnectResult;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.networking.SendMovesResult;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.service.TimerService;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.ui.dialogs.CancelActionDialogFragment;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.ui.dialogs.ChooseActionDialogFragment;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.ui.dialogs.CustomProgressDialog;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.ui.dialogs.GameOverDialogFragment;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.ui.dialogs.ListPlayersDialogFragment;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.ui.dialogs.MessageDialogFragment;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.ui.dialogs.ObjectiveDialogFragment;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.ui.entities.HouseToken;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.ui.enums.SelectionState;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.util.ConnectionDetector;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.util.Constants;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.util.RulesChecker;
import br.ufcg.edu.ccc.projeto2.R;

/**
 * 
 * @author Arnett
 * 
 */
public class GameActivity extends Activity implements OnTouchListener,
OnActionSelectedListener, OnClickListener, OnTaskCompleted {

	private static final int SOLVE_CONFLICT_RETURN = 1;

	private boolean isOpenningConflictActivity = false; // to just close the
	// waitDialog when the
	// activity is started

	private String LOG_TAG = "GameActivity";

	private String CHOOSE_ACTION_DIALOG_FRAGMENT_TAG = "ChooseActionDialogFragmentTag";
	private String GAME_OVER_DIALOG_FRAGMENT_TAG = "GameOverDialogFragmentTag";
	private String LIST_PLAYERS_DIALOG_FRAGMENT_TAG = "ListPlayersDialogFragmentTag";
	private static final String CANCEL_ACTION_DIALOG_FRAGMENT_TAG = "CancelActionDialogFragmentTag";

	private RelativeLayout tokenLayout;
	private Bitmap imageToken;

	private ImageView hint;
	private AnimationDrawable hintAnimation;
	private boolean alreadyDrawnHint = false;
	private int round = 0;
	
	private View mapImage;
	private View mapImageMask;
	private Bitmap maskImageBitmap;
	private ImageView nextPhaseButton;
	private ImageView objectiveButton;
	private ImageView listPlayersButton;
	private TextView timeCounter;

	private SelectionState currentActionSelectionState = SelectionState.SELECTING_ORIGIN;

	private GameManager gameManager;
	private CommunicationManager communicationManager;
	private TerritoryUIManager territoryManager;
	private HouseTokenManager houseTokenManager;
	private ProfileManager profileManager;

	private Territory firstSelectedTerritoryForTheCurrentMove;

	private ImageView currentPlayerToken;

	private CustomProgressDialog waitDialog;

	private ChooseActionDialogFragment chooseActionDialogFragment;

	private ObjectiveDialogFragment objectiveDialogFragment;
	private ListPlayersDialogFragment listPlayersDialogFragment;
	
	private boolean areFirstTokensDrawn = true;
	private SendMovesResult sendMovesResult;
	private boolean updateUI = true;
	private boolean gameFinished;
	private boolean countdownIsRunning;
	

	//	TODO MOVE TO ANOTHER CLASS
	private String[] NorthTerritories = new String[] {"A", "B", "C", "D", "E", "F", "G", "H"};
	private String[] CenterTerritories = new String[] {"I", "J", "K", "L", "M", "N", "O", "P", "Q"};
	private String[] SouthTerritories = new String[] {"R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {            
			updateGUI(intent); // or whatever method used to update your GUI fields
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		gameManager = GameManager.getInstance();
		gameManager.updateCurrentPlayerName(getUserName());
		
		territoryManager = TerritoryUIManager.getInstance();
		houseTokenManager = HouseTokenManager.getInstance();
		profileManager = ProfileManager.getInstance();
		communicationManager = NetworkManager.getInstance();
		setContentView(R.layout.map_screen);
		
		waitDialog = new CustomProgressDialog(this, R.drawable.progress, getString(R.string.waiting_players_moves));

		mapImage = findViewById(R.id.map);
		mapImageMask = findViewById(R.id.map_mask);

		nextPhaseButton = (ImageView) findViewById(R.id.nextPhaseButton);
		objectiveButton = (ImageView) findViewById(R.id.objectiveButton);
		listPlayersButton = (ImageView) findViewById(R.id.listPlayersButton);
		timeCounter = (TextView) findViewById(R.id.time_counter);
		tokenLayout = (RelativeLayout) findViewById(R.id.token);
		tokenLayout.setBackgroundColor(100);
		
		hint = (ImageView) findViewById(R.id.hint);

		mapImage.setOnTouchListener(this);
		nextPhaseButton.setOnClickListener(this);
		objectiveButton.setOnClickListener(this);
		listPlayersButton.setOnClickListener(this);

		currentPlayerToken = (ImageView) findViewById(R.id.currentPlayerToken);
		drawCurrentPlayerToken();
		setCurrentPlayerHomebase();

		mapImageMask.addOnLayoutChangeListener(new OnLayoutChangeListener() {
			@Override
			public void onLayoutChange(View v, int left, int top, int right,
					int bottom, int oldLeft, int oldTop, int oldRight,
					int oldBottom) {
				reloadMaskImageBitmap();
			}
		});

		startService(new Intent(this, TimerService.class));
		Log.v(LOG_TAG, "registering receiver - on create");
		registerReceiver(broadcastReceiver, new IntentFilter(TimerService.COUNTDOWN));
		countdownIsRunning = true;
		showObjectiveDialog(false, 0, 0, 0, false);
		
		round++;
	}
	
	private String getUserName() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this.getApplicationContext());
		String userName = sharedPreferences.getString("user_name", "Anonymous Player");
		Log.v(LOG_TAG, userName);
		return userName;
	}

	private void setCurrentPlayerHomebase() {
		House currentPlayerHouse = gameManager.getCurrentPlayer().getHouse();
		// this method is only called on the onCreate method, meaning that the list will only have one element
		Territory currentPlayerHomebase = gameManager.getTerritories(currentPlayerHouse).get(0);

		gameManager.setCurrentPlayerHomebase(currentPlayerHomebase);
	}

	/*
	 * Opens dialog fragment with objective info
	 * 
	 * @params fromOnCreate - if is the first time is openning (from oncreate)
	 */
	private void showObjectiveDialog(boolean withData, int numNorth, int numCenter, int numSouth, boolean hasHomebase) {

		objectiveDialogFragment = new ObjectiveDialogFragment();

		if (withData) {
			Bundle data = new Bundle();
			data.putInt(Constants.NUM_CONQUERED_NORTH, numNorth);
			data.putInt(Constants.NUM_CONQUERED_CENTER, numCenter);
			data.putInt(Constants.NUM_CONQUERED_SOUTH, numSouth);
			data.putBoolean(Constants.HAS_HOMEBASE, hasHomebase);
			objectiveDialogFragment.setArguments(data);
		}

		objectiveDialogFragment.show(getFragmentManager(), "objectiveDialog");
	}

	private void drawCurrentPlayerToken() {
		House currentHouse = GameManager.getInstance().getCurrentPlayer()
				.getHouse();
		HouseToken playerHouseToken = houseTokenManager
				.getHouseToken(currentHouse);
		currentPlayerToken.setImageResource(playerHouseToken.getCastleToken());
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		/**
		 * The map image is only plotted when the activity is loaded, i.e. when
		 * the state of the activity is about to become "Running". Since the
		 * centers of the territories are calculated based on the current size
		 * of the map, the territory centers don't exist before this.
		 */
		if (areFirstTokensDrawn) {
			drawTerritoryOwnershipTokens();
			areFirstTokensDrawn = false;
		} 
		
		if (round == 1 && !alreadyDrawnHint) {
			startHintAnimation();
			alreadyDrawnHint = true;
		}
	}

	private void stopHintAnimation() {
		if (hintAnimation != null) {
			hintAnimation.stop();
			hint.setImageResource(R.drawable.clean_map);
		}
	}

	private void startHintAnimation() {
		
		// TODO - why in some cases hint is null?????
		if (hint != null) {
			House currentPlayerHouse = gameManager.getCurrentPlayer().getHouse();
			
			if (currentPlayerHouse.getName().equals("Stark")) {
				hint.setImageResource(R.drawable.stark_hint_animation);
			}
			else if (currentPlayerHouse.getName().equals("Lannister")) {
				hint.setImageResource(R.drawable.lannister_hint_animation);
			}
			else if (currentPlayerHouse.getName().equals("Martell")) {
				hint.setImageResource(R.drawable.martell_hint_animation);
			}
			
			hintAnimation = (AnimationDrawable) hint.getDrawable();
			hintAnimation.start();
		}
	}

	/**
	 * Method to recreate the maskImageBitmap, it is used when the layout
	 * changes (mainly for screen rotation).
	 */
	private void reloadMaskImageBitmap() {
		mapImageMask.setDrawingCacheEnabled(true);
		maskImageBitmap = Bitmap.createBitmap(mapImageMask.getDrawingCache());
		mapImageMask.setDrawingCacheEnabled(false);
	}

	/**
	 * Returns the color of the pixel <tt>(x, y)</tt> on the image represented
	 * by <tt>hotspotId</tt>
	 * 
	 * @param activity
	 *            the activity that contains the mask image
	 * @param hotspotId
	 *            the hotspot id of the mask image
	 * @param x
	 *            the x position of the pixel to be look at
	 * @param y
	 *            the y position of the pixel to be look at
	 * @return
	 */
	private int getHotspotColor(int hotspotId, int x, int y) {
		
		int pixelColor;
		try {
			pixelColor = maskImageBitmap.getPixel(x, y);
		} catch (Exception e) {
			pixelColor = Color.WHITE;
		}
		
		return pixelColor;
	}

	private void addTokenToLayout(int imageResource, int x, int y,
			RelativeLayout layout) {
		imageToken = BitmapFactory.decodeResource(this.getResources(),
				imageResource);

		// Centralize the image
		x -= imageToken.getWidth() / 2;
		y -= imageToken.getHeight() / 2;

		ImageView imageView = new ImageView(this);
		imageView.setImageBitmap(imageToken);

		// Parameter to place the image in the right place, centered in x, y
		// Create a param object with same dimensions as imageToken
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				imageToken.getWidth(), imageToken.getHeight());
		// The image will be placed 'x' units far from the left border
		// and y far from the top border
		params.leftMargin = x;
		params.topMargin = y;

		layout.addView(imageView, params);
	}

	private void drawTerritoryOwnershipTokens() {
		tokenLayout.removeAllViews();
		for (Territory territory : gameManager.getAllTerritories()) {
			if (!territory.isFree()) {
				int tokenImage = getTokenImage(territory);
				int centerX = TerritoryUIManager.getInstance()
						.getTerritoryUICenter(territory)
						.getCenterX(getMapWidth());
				int centerY = TerritoryUIManager.getInstance()
						.getTerritoryUICenter(territory)
						.getCenterY(getMapHeight());
				addTokenToLayout(tokenImage, centerX, centerY, tokenLayout);
			}
		}
	}

	private void drawActionTokens() {
		for (Move move : gameManager.getCurrentMoves()) {
			if (move.getAction().equals(Action.ATTACK)) {
				int xTerritoryCenter = territoryManager.getTerritoryUICenter(
						move.getTarget()).getCenterX(getMapWidth());
				int yTerritoryCenter = territoryManager.getTerritoryUICenter(
						move.getTarget()).getCenterY(getMapHeight());
				addTokenToLayout(R.drawable.token_attack, xTerritoryCenter,
						yTerritoryCenter, tokenLayout);
			}
			else if (move.getAction().equals(Action.DEFEND)) {
				int xTerritoryCenter = territoryManager.getTerritoryUICenter(
						move.getTarget()).getCenterX(getMapWidth());
				int yTerritoryCenter = territoryManager.getTerritoryUICenter(
						move.getTarget()).getCenterY(getMapHeight());
				addTokenToLayout(R.drawable.token_defense, xTerritoryCenter,
						yTerritoryCenter, tokenLayout);
			}
		}
	}

	private int getTokenImage(Territory territory) {
		return houseTokenManager.getTokenImage(territory);
	}

	@Override
	public boolean onTouch(View touchedView, MotionEvent event) {
		double DEBUG_StartTime = System.nanoTime();

		int action = event.getAction();
		int motionEventX = (int) event.getX();
		int motionEventY = (int) event.getY();

		int touchedPixelColor = getHotspotColor(R.id.map_mask, motionEventX,
				motionEventY);

		Log.v(LOG_TAG,
				"touched pixel color is "
						+ Integer.toHexString(touchedPixelColor));

		// When the user touches outside the image itself, onTouch is called
		// because the ImageView matches its parent, i.e. the whole screen, but
		// the image is not stretched to match the whole screen
		if (touchedPixelColor == 0 || touchedPixelColor == -1) {
			return false;
		}

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			// TODO Change the texture of the territory where the user touched
			// to pressed_territory, as we would do with buttons.
			break;
		case MotionEvent.ACTION_UP:
			processTouch(motionEventX, motionEventY, touchedPixelColor);
			break;
		}

		Log.v(LOG_TAG, "onTouch total time was "
				+ (System.nanoTime() - DEBUG_StartTime) / 1000000);
		return true;
	}

	private void processTouch(int motionEventX, int motionEventY,
			int touchedPixelColor) {

		Log.d(LOG_TAG, "touched position X "
				+ motionEventX +" Y "+motionEventY);

		switch (currentActionSelectionState) {
		case SELECTING_ORIGIN:
			processFirstTouch(motionEventX, motionEventY, touchedPixelColor);
			break;
		case SELECTING_TARGET:
			processSecondTouch(motionEventX, motionEventY, touchedPixelColor);
			break;
		}
	}

	private void processFirstTouch(int motionEventX, int motionEventY,
			int touchedPixelColor) {
		
		stopHintAnimation();
		
		String touchedTerritoryName = territoryManager
				.getTerritoryByClosestColor(touchedPixelColor);

		firstSelectedTerritoryForTheCurrentMove = gameManager
				.getTerritoryByName(touchedTerritoryName);

		Log.d(LOG_TAG, "touched territory is "
				+ firstSelectedTerritoryForTheCurrentMove);

		RulesChecker rulesChecker = RulesChecker.getInstance();

		if(rulesChecker.isTerritoryAlreadyATarget(firstSelectedTerritoryForTheCurrentMove)) {
			startCancelMovePopup();
		}
		else if (rulesChecker
				.isTerritoryAlreadyAnOrigin(firstSelectedTerritoryForTheCurrentMove)) {
			
			showInfoToast(getString(R.string.territory_already_used_during_this_turn_info));
		}

		else if (!rulesChecker
				.isTerritoryOwnedByTheCurrentPlayer(firstSelectedTerritoryForTheCurrentMove)) {
			
			showInfoToast(getString(R.string.choose_a_territory_owned_by_you_info));
		}

		else if (rulesChecker.isTerritoryOwnedByTheCurrentPlayer(firstSelectedTerritoryForTheCurrentMove) && 
				!rulesChecker.isTerritoryAlreadyAnOrigin(firstSelectedTerritoryForTheCurrentMove)){

			Action[] applicableActionsToThisTerritory = gameManager
					.getApplicableActions(firstSelectedTerritoryForTheCurrentMove);

			startActionPopup(applicableActionsToThisTerritory);
		}
	}

	private void processSecondTouch(int motionEventX, int motionEventY,
			int touchedPixelColor) {
		String touchedTerritoryName = territoryManager
				.getTerritoryByClosestColor(touchedPixelColor);

		Territory touchedTerritory = gameManager
				.getTerritoryByName(touchedTerritoryName);

		RulesChecker rulesChecker = RulesChecker.getInstance();

		if (rulesChecker.isTerritoryOwnedByTheCurrentPlayer(touchedTerritory)) {
			showInfoToast(getString(R.string.territory_already_owned_by_you_info));
		} else if (!rulesChecker.isTargetAdjacentToOrigin(
				firstSelectedTerritoryForTheCurrentMove, touchedTerritory)) {
			showInfoToast(getString(R.string.attack_a_neighbour_territory_info));
		} else if (rulesChecker.isTerritoryAlreadyATarget(touchedTerritory)) {
			showInfoToast(getString(R.string.you_already_attacked_this_territory_info));
		} else if (rulesChecker.isTargetAdjacentToOrigin(firstSelectedTerritoryForTheCurrentMove, touchedTerritory) && 
				!rulesChecker.isTerritoryOwnedByTheCurrentPlayer(touchedTerritory) && !rulesChecker.isTerritoryAlreadyATarget(touchedTerritory)){
			
			gameManager.makeAttackMove(firstSelectedTerritoryForTheCurrentMove,
					touchedTerritory);

			int xTerritoryCenter = territoryManager.getTerritoryUICenter(
					touchedTerritory).getCenterX(getMapWidth());
			int yTerritoryCenter = territoryManager.getTerritoryUICenter(
					touchedTerritory).getCenterY(getMapHeight());
			addTokenToLayout(R.drawable.token_attack, xTerritoryCenter,
					yTerritoryCenter, tokenLayout);
		}
		currentActionSelectionState = SelectionState.SELECTING_ORIGIN;
		stopHintAnimation();
	}

	private void startActionPopup(Action[] actions) {
		chooseActionDialogFragment = new ChooseActionDialogFragment();
		chooseActionDialogFragment.setActions(actions);
		chooseActionDialogFragment.show(getFragmentManager(),
				CHOOSE_ACTION_DIALOG_FRAGMENT_TAG);
	}

	@Override
	public void onActionSelected(Action chosenAction) {
		switch (chosenAction) {
		case ATTACK:
			currentActionSelectionState = SelectionState.SELECTING_TARGET;
			
			if (round == 1) {
				startNeighbourHintAnimation();
			}
			break;
		case DEFEND:
			gameManager.makeDefendMove(firstSelectedTerritoryForTheCurrentMove);

			int xTerritoryCenter = territoryManager.getTerritoryUICenter(
					firstSelectedTerritoryForTheCurrentMove).getCenterX(
							getMapWidth());
			int yTerritoryCenter = territoryManager.getTerritoryUICenter(
					firstSelectedTerritoryForTheCurrentMove).getCenterY(
							getMapHeight());
			addTokenToLayout(R.drawable.token_defense, xTerritoryCenter,
					yTerritoryCenter, tokenLayout);

			currentActionSelectionState = SelectionState.SELECTING_ORIGIN;
			break;
		case CANCEL:
			Move moveToRemove = null;
			List<Move> currentMoves = gameManager.getCurrentMoves();
			for (Move move : currentMoves) {
				if(move.getTarget().equals(firstSelectedTerritoryForTheCurrentMove)) {
					moveToRemove = move;
				}
			}
			gameManager.getCurrentMoves().remove(moveToRemove);
			drawTerritoryOwnershipTokens();
			drawActionTokens();	
			currentActionSelectionState = SelectionState.SELECTING_ORIGIN;
			break;
		}
	}

	private void startNeighbourHintAnimation() {
		
		// TODO - why in some cases hint is null?????
		if (hint != null) {
			House currentPlayerHouse = gameManager.getCurrentPlayer().getHouse();
			
			if (currentPlayerHouse.getName().equals("Stark")) {
				hint.setImageResource(R.drawable.stark_neighbour_hint_animation);
			}
			else if (currentPlayerHouse.getName().equals("Lannister")) {
				hint.setImageResource(R.drawable.lannister_neighbour_hint_animation);
			}
			else if (currentPlayerHouse.getName().equals("Martell")) {
				hint.setImageResource(R.drawable.martell_neighbour_hint_animation);
			}
			
			hintAnimation = (AnimationDrawable) hint.getDrawable();
			hintAnimation.start();
		}
	}

	private void startCancelMovePopup() {
		CancelActionDialogFragment cancelActionDialogFragment = new CancelActionDialogFragment();
		cancelActionDialogFragment.show(getFragmentManager(),
				CANCEL_ACTION_DIALOG_FRAGMENT_TAG);
	}

	private boolean contains(String[] array, String element) {
		for (String string : array)
			if (string.equalsIgnoreCase(element))
				return true;
		return false;
	}

	private void startNextPhase() {
		ConnectionDetector connectionDetector = new ConnectionDetector(
				getApplicationContext());
		if (connectionDetector.isConnectingToInternet()) {
			Log.v(LOG_TAG, "Unregistering receiver - start next phase");
			unregisterReceiver(broadcastReceiver);
			stopService(new Intent(this, TimerService.class));
			countdownIsRunning = false;
			dismissDialogs();
			waitDialog.show();
			communicationManager
			.sendCurrentMoves(this, gameManager.getCurrentMoves());
			gameManager.startNextPhase();
		} else {
			waitDialog.dismiss();
			openMessageDialog(
					getResources().getString(R.string.sorry_label),
					getResources().getString(R.string.no_internet_msg), 
					Constants.DIALOG_ERROR);
		}
	}

	private void dismissDialogs() {
		try {
			waitDialog.dismiss();
			objectiveDialogFragment.dismiss();
			listPlayersDialogFragment.dismiss();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.nextPhaseButton:
			startNextPhase();
			break;
		case R.id.objectiveButton:
			openGameObjective();
			break;
		case R.id.listPlayersButton:
			openListPlayers();
			break;
		default:
			return;
		}
	}

	private void openListPlayers() {
		listPlayersDialogFragment = new ListPlayersDialogFragment();
		listPlayersDialogFragment.show(getFragmentManager(), LIST_PLAYERS_DIALOG_FRAGMENT_TAG);
	}

	/**
	 * Opens a message dialog
	 * 
	 * @param header dialog title
	 * @param message
	 * @param type - error or info
	 */
	private void openMessageDialog(String msgHeader, String message, int type) {

		MessageDialogFragment msgDialog = new MessageDialogFragment();
		Bundle args = new Bundle();
		
		args.putString(Constants.DIALOG_MESSAGE_HEADER, msgHeader);
		args.putString(Constants.DIALOG_MESSAGE, message);
		args.putInt(Constants.DIALOG_TYPE, type);
		msgDialog.setArguments(args);
		
		try {
			msgDialog.show(getFragmentManager(), "msgDialog");
		} catch (Exception e) {
			// do nothing
		}
	}

	private void openGameObjective() {
		int north = 0, center = 0, south = 0;
		for (Territory territory : gameManager.getAllTerritories()) {
			if (!territory.isFree() && territory.getOwner().getName().equals(gameManager.getCurrentPlayer().getHouse().getName())) {
				if (contains(NorthTerritories, territory.getName()))
					north++;
				else if (contains(CenterTerritories, territory.getName()))
					center++;
				else if (contains(SouthTerritories, territory.getName()))
					south++;
			}
		}

		House currentPlayerHouse = gameManager.getCurrentPlayer().getHouse();

		// if the current player still has his homebase
		boolean hasHomebase = gameManager.getCurrentPlayerHomebase().getOwner().equals(currentPlayerHouse);

		showObjectiveDialog(true, north, center, south, hasHomebase);
	}

	@Override
	public void onSendMovesTaskCompleted(SendMovesResult result) {
		round++;
		sendMovesResult = result;
		if(sendMovesResult == null) {
			waitDialog.dismiss();
			openMessageDialog(
					getResources().getString(R.string.sorry_label),
					getResources().getString(R.string.internet_missbehaving_msg), 
					Constants.DIALOG_ERROR);
		}
		else {
			if (chooseActionDialogFragment != null && chooseActionDialogFragment.isVisible()) {
				chooseActionDialogFragment.dismiss();
			}
			firstSelectedTerritoryForTheCurrentMove = null;

			if (result.getConflicts() != null && 
					result.getConflicts().size() != 0 && 
					currentPlayerIsInvolved(result.getConflicts())) {

				isOpenningConflictActivity = true;

				Intent intent = new Intent(this, ConflictActivity.class);
				Bundle bundle = new Bundle();
				bundle.putParcelableArrayList("conflicts",
						(ArrayList<? extends Parcelable>) result.getConflicts());
				intent.putExtras(bundle);
				startActivityForResult(intent, SOLVE_CONFLICT_RETURN);

			} else {
				isOpenningConflictActivity = false;
				doActionsAfterSendMovesReturned();
			}

			if (!isOpenningConflictActivity && !gameFinished) {
				waitDialog.dismiss();
				resetCountDown();
			}

		}
		firstSelectedTerritoryForTheCurrentMove = null;
		currentActionSelectionState = SelectionState.SELECTING_ORIGIN;
	}

	/*
	 * Verify if the current player is involved in the current conflicts
	 */
	private boolean currentPlayerIsInvolved(List<Conflict> conflicts) {

		for (Conflict conflict : conflicts) {
			for (House house : conflict.getHouses()) {

				if (house.equals(gameManager.getCurrentPlayer().getHouse())) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if (isOpenningConflictActivity) {
			waitDialog.dismiss();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == SOLVE_CONFLICT_RETURN) {
			doActionsAfterSendMovesReturned();
			isOpenningConflictActivity = false;
			waitDialog.dismiss();
			resetCountDown();
		}
	}

	private void resetCountDown() {
		stopService(new Intent(this, TimerService.class));
		startService(new Intent(this, TimerService.class));
		Log.v(LOG_TAG, "registering receiver - reset countdown");
		registerReceiver(broadcastReceiver, new IntentFilter(TimerService.COUNTDOWN));
		countdownIsRunning = true;
	}

	private void doActionsAfterSendMovesReturned() {
		gameManager.updateAllTerritories(sendMovesResult.getUpdatedMap());
		drawTerritoryOwnershipTokens();

		if (sendMovesResult.getGameState().isWO()) {
			gameFinished = true;
			
			List<Player> winners = new ArrayList<Player>();
			winners.add(gameManager.getCurrentPlayer());
			openGameFinishedDialg(
					winners, 
					sendMovesResult.getGameState().isWO());
		}
		else if (sendMovesResult.getGameState().isGameFinished()) {
			profileManager.saveGameStatistics(sendMovesResult.getGameState(), getApplicationContext());
			gameFinished = true;
			openGameFinishedDialg(
					sendMovesResult.getGameState().getWinnerList(), 
					sendMovesResult.getGameState().isWO());
		}
	}

	private void openGameFinishedDialg(List<Player> winners, boolean wo) {
		GameOverDialogFragment gameOverDialog = new GameOverDialogFragment();
		gameOverDialog.setWinners(winners);
		gameOverDialog.setWO(wo);
		
		try {
			gameOverDialog.show(
					getFragmentManager(), 
					GAME_OVER_DIALOG_FRAGMENT_TAG);
			waitDialog.dismiss();
		} catch (Exception e) {
			// do nothing
		}
	}

	@Override
	protected void onDestroy() {

		if (countdownIsRunning) {
			unregisterReceiver(broadcastReceiver);
			stopService(new Intent(this, TimerService.class));
			countdownIsRunning = false;
		}
		
		if (waitDialog.isShowing()) {
			waitDialog.dismiss();
		}

		Log.v(LOG_TAG, "Destroying activity");

		super.onDestroy();
	}

	@Override
	public void onConnectTaskCompleted(ConnectResult result) {
		// TODO Remove this method from OnTaskCompleted and create a global
		// interface with a request id (as we do in startActivityForResult(act,
		// i)) or create multiple listeners, each for a single asynchronous
		// task.
	}

	private int getMapWidth() {
		ImageView mapImageView = (ImageView) mapImage;
		return mapImageView.getMeasuredWidth();
	}

	private int getMapHeight() {
		ImageView mapImageView = (ImageView) mapImage;
		return mapImageView.getMeasuredHeight();
	}

	private void updateGUI(Intent intent) {
		if (intent.getExtras() != null) {
			long millisUntilFinished = intent.getLongExtra("countdown", TimerService.START_TIME);
			if (updateUI) {
				updateTimerText(millisUntilFinished);
			}
			if (millisUntilFinished == 0) {
				startNextPhase();
			}

		}
	}

	private void updateTimerText(long millisUntilFinished) {
		long seconds = millisUntilFinished / 1000;
		long formatedSeconds = seconds % 60;
		long minutes = seconds / 60;
		if (seconds <= 10) {
			timeCounter.setTextColor(Color.parseColor("#FF0000"));
		} else {
			timeCounter.setTextColor(Color.parseColor("#000000"));
		}
		timeCounter.setText(String.format("%02d", minutes) + ":"
				+ String.format("%02d", formatedSeconds));
	}
	
	private void showInfoToast(String message) {
		LayoutInflater inflater = getLayoutInflater();

		View layout = inflater.inflate(R.layout.custom_toast,
                                       (ViewGroup) findViewById(R.id.toast_layout_root));

        TextView text = (TextView) layout.findViewById(R.id.message);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.TOP, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
	}
}