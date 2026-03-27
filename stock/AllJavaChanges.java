// ═══════════════════════════════════════════════════════════════════════════════
// SplashActivity.java  — MINIMAL CHANGE: swap setContentView for setContent
// ═══════════════════════════════════════════════════════════════════════════════

// BEFORE:
//   setContentView(R.layout.activity_splash);
//
// AFTER (in onCreate, replace that one line):
//   SplashBridgeKt.setupComposeUI(this);
//
// Everything else (Handler, routing logic) stays EXACTLY the same.
// The Compose SplashScreen just shows the animated UI; routing stays in Java.


// ═══════════════════════════════════════════════════════════════════════════════
// SafetyActivity.java  — CHANGES SUMMARY
// ═══════════════════════════════════════════════════════════════════════════════

public class SafetyActivity extends AppCompatActivity {

    // REMOVE: private ActivitySafetyBinding binding;
    private SafetyStateHolder stateHolder;   // ADD
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // REMOVE: binding = ActivitySafetyBinding.inflate(...)
        // REMOVE: setContentView(binding.getRoot())
        // REMOVE: binding.btnIUnderstand.setOnClickListener(...)

        session = new SessionManager(this);
        stateHolder = new SafetyStateHolder();
        SafetyBridgeKt.setupComposeUI(this, stateHolder);   // ADD
    }

    // ADD this public method — called by the bridge
    public void handleIUnderstand() {
        if (!stateHolder.isChecked()) {
            return; // Already validated in Compose
        }
        session.setSafetyShown(true);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// PhoneAuthActivity.java  — CHANGES SUMMARY
// ═══════════════════════════════════════════════════════════════════════════════

public class PhoneAuthActivity extends AppCompatActivity {

    // REMOVE: private ActivityPhoneAuthBinding binding;
    private PhoneAuthStateHolder stateHolder;   // ADD

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // REMOVE: binding = ...
        // REMOVE: setContentView(...)
        // REMOVE: binding.btnContinue.setOnClickListener(...)

        stateHolder = new PhoneAuthStateHolder();
        PhoneAuthBridgeKt.setupComposeUI(this, stateHolder);   // ADD
    }

    // ADD — called by bridge on Continue tap
    public void handleContinue(String fullPhone) {
        if (fullPhone.trim().isEmpty() || fullPhone.trim().length() < 10) {
            Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, OtpVerifyActivity.class);
        intent.putExtra(AppConstants.KEY_PHONE_NUMBER, fullPhone);
        startActivity(intent);
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// OtpVerifyActivity.java  — CHANGES SUMMARY
// ═══════════════════════════════════════════════════════════════════════════════

public class OtpVerifyActivity extends AppCompatActivity {

    // REMOVE: private ActivityOtpVerifyBinding binding;
    private OtpVerifyStateHolder stateHolder;   // ADD

    // Everything else (verificationId, resendToken, auth, phoneNumber, session) STAYS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // REMOVE: binding = ...
        // REMOVE: setContentView(...)
        // REMOVE: binding.tvPhoneHint.setText(...)
        // REMOVE: binding.btnVerify.setOnClickListener(...)
        // REMOVE: binding.tvResend.setOnClickListener(...)

        auth = FirebaseManager.getInstance().getAuth();
        session = new SessionManager(this);
        phoneNumber = getIntent().getStringExtra(AppConstants.KEY_PHONE_NUMBER);

        stateHolder = new OtpVerifyStateHolder();
        stateHolder.setPhoneNumber(phoneNumber != null ? phoneNumber : "");
        OtpVerifyBridgeKt.setupComposeUI(this, stateHolder);   // ADD

        sendOtp(phoneNumber, false);  // STAYS
    }

    // ADD — called by bridge
    public void handleVerify(String code) {
        if (code.length() != 6) return;
        verifyCode(code);  // calls existing private method
    }

    // ADD — called by bridge
    public void handleResend() {
        sendOtp(phoneNumber, true);  // calls existing private method
    }

    // CHANGE: replace binding.progressBar.setVisibility / binding.btnVerify.setEnabled
    private void setLoading(boolean loading) {
        stateHolder.setLoading(loading);   // was: binding.progressBar + binding.btnVerify
    }

    // CHANGE: replace binding.tvResend.setText / setEnabled
    private void startResendTimer() {
        stateHolder.setResendSeconds(60);
        new CountDownTimer(60000, 1000) {
            public void onTick(long ms) {
                stateHolder.setResendSeconds((int)(ms / 1000));  // was: binding.tvResend.setText
            }
            public void onFinish() {
                stateHolder.setResendSeconds(0);   // was: binding.tvResend.setEnabled(true)
            }
        }.start();
    }

    // All private logic (sendOtp, verifyCode, signInWithCredential, etc.) UNCHANGED
}


// ═══════════════════════════════════════════════════════════════════════════════
// ChatActivity.java  — CHANGES SUMMARY
// ═══════════════════════════════════════════════════════════════════════════════

public class ChatActivity extends AppCompatActivity implements ChatPresenter.ChatView {

    // REMOVE: private ActivityChatBinding binding;
    // REMOVE: private ChatAdapter adapter;
    // REMOVE: LinearLayoutManager
    private ChatStateHolder stateHolder;   // ADD

    // KEEP: presenter, currentUid, chatRoomId, session

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        chatRoomId    = getIntent().getStringExtra(AppConstants.KEY_CHAT_ROOM_ID);
        String otherUserName = getIntent().getStringExtra(AppConstants.KEY_OTHER_USER_NAME);
        currentUid    = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        session = new SessionManager(this);

        stateHolder = new ChatStateHolder();
        // Get disclaimer from strings.xml
        String disclaimer = getString(R.string.chat_disclaimer);
        ChatBridgeKt.setupComposeUI(this, stateHolder,
                otherUserName != null ? otherUserName : "Chat", disclaimer);   // ADD

        presenter = new ChatPresenter(this, chatRoomId);
        presenter.startListening();
    }

    // ADD — called by Compose send button
    public void handleSend(String text) {
        if (text.isEmpty()) return;
        ChatMessage msg = new ChatMessage(currentUid, session.getUserName(), text, ChatMessage.TYPE_TEXT);
        presenter.sendMessage(msg);
    }

    @Override
    public void onMessageReceived(com.safar.app.models.ChatMessage message) {
        // Convert your model to UI model and add to state
        com.safar.app.views.ui.screens.ChatMessage uiMsg =
            new com.safar.app.views.ui.screens.ChatMessage(
                message.getMessageId() != null ? message.getMessageId() : String.valueOf(System.currentTimeMillis()),
                message.getText(),
                message.getSenderId() != null && message.getSenderId().equals(currentUid),
                com.safar.app.utils.DateUtils.formatTime(message.getTimestamp())
            );
        stateHolder.addMessage(uiMsg);   // was: adapter.addMessage + scrollToPosition
    }

    // onMessagesLoaded, onMessageSent, onError, onDestroy — UNCHANGED
}


// ═══════════════════════════════════════════════════════════════════════════════
// ProfileActivity.java  — CHANGES SUMMARY
// ═══════════════════════════════════════════════════════════════════════════════

public class ProfileActivity extends AppCompatActivity {

    // REMOVE: private ActivityProfileBinding binding;
    private ProfileStateHolder stateHolder;   // ADD
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        stateHolder = new ProfileStateHolder();
        ProfileBridgeKt.setupComposeUI(this, stateHolder);   // ADD

        loadProfile();  // STAYS — but now updates stateHolder instead of binding
    }

    private void loadProfile() {
        stateHolder.setName(session.getUserName() != null ? session.getUserName() : "");
        stateHolder.setPhone(session.getUserPhone() != null ? session.getUserPhone() : "");

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        FirebaseManager.getInstance().getUserRef(uid).get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        stateHolder.setName(user.getName());     // was: binding.tvName.setText
                        stateHolder.setPhone(user.getPhone());   // was: binding.tvPhone.setText
                        stateHolder.setTotalRides(user.getTotalRides());
                        if (user.getRatingCount() > 0) {
                            stateHolder.setRating((float) user.getRating(), user.getRatingCount());
                        }
                    }
                });
    }

    // ADD
    public void navigateToMyRides() {
        startActivity(new Intent(this, MyAdsActivity.class));
    }

    // ADD
    public void showLogoutDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (d, w) -> {
                    session.logout(this);
                    Intent i = new Intent(this, PhoneAuthActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// MyAdsActivity.java  — CHANGES SUMMARY
// ═══════════════════════════════════════════════════════════════════════════════

public class MyAdsActivity extends AppCompatActivity implements AdsPresenter.AdsView {

    // REMOVE: private ActivityMyAdsBinding binding;
    // REMOVE: private RideAdAdapter adapter;
    private MyAdsStateHolder stateHolder;   // ADD
    private AdsPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stateHolder = new MyAdsStateHolder();
        MyAdsBridgeKt.setupComposeUI(this, stateHolder);   // ADD

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        presenter = new AdsPresenter(this);
        if (uid != null) presenter.loadMyAds(uid);
    }

    // ADD
    public void navigateToAdDetail(String adId) {
        Intent i = new Intent(this, AdDetailActivity.class);
        i.putExtra(AppConstants.KEY_AD_ID, adId);
        startActivity(i);
    }

    @Override
    public void onAdsLoaded(List<RideAd> ads) {
        // Convert RideAd → RideAdUi
        List<RideAdUi> uiList = new ArrayList<>();
        for (RideAd ad : ads) {
            uiList.add(new RideAdUi(
                ad.getAdId(),
                ad.getOrigin(),
                ad.getDestination(),
                ad.getDepartureTime(),
                ad.getSeats(),
                ad.getVehicle() != null ? ad.getVehicle() : "",
                ad.getStatus() != null ? ad.getStatus() : "Open"
            ));
        }
        stateHolder.setAds(uiList);          // was: adapter.updateList + binding.tvEmpty
    }

    @Override
    public void onLoading(boolean isLoading) {
        stateHolder.setLoading(isLoading);   // was: binding.progressBar
    }

    // onAdPosted, onAdDeleted, onError — UNCHANGED
}


// ═══════════════════════════════════════════════════════════════════════════════
// PostAdActivity.java  — CHANGES SUMMARY
// ═══════════════════════════════════════════════════════════════════════════════

public class PostAdActivity extends AppCompatActivity implements AdsPresenter.AdsView {

    // REMOVE: private ActivityPostAdBinding binding;
    private PostAdStateHolder stateHolder;   // ADD

    // KEEP: presenter, selectedDateTime, dateTimeSelected, originLat/Lng, destLat/Lng,
    //       originPicked, destPicked, originPickerLauncher, destPickerLauncher
    // These are all still Java-controlled

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        presenter   = new AdsPresenter(this);
        session     = new SessionManager(this);
        stateHolder = new PostAdStateHolder();
        PostAdBridgeKt.setupComposeUI(this, stateHolder);   // ADD
    }

    // ADD — called by bridge (replaces binding.etOrigin.setOnClickListener)
    public void launchOriginPicker() {
        Intent i = new Intent(this, LocationPickerActivity.class);
        i.putExtra(LocationPickerActivity.EXTRA_TITLE, "Pick Origin");
        originPickerLauncher.launch(i);
    }

    // ADD — called by bridge
    public void launchDestPicker() {
        Intent i = new Intent(this, LocationPickerActivity.class);
        i.putExtra(LocationPickerActivity.EXTRA_TITLE, "Pick Destination");
        destPickerLauncher.launch(i);
    }

    // ADD — called by bridge (replaces binding.btnPickDateTime.setOnClickListener)
    public void showDatePicker() {
        // EXACT same DatePickerDialog code from original — unchanged
        Calendar now = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, day);
                    showTimePicker();
                },
                now.get(Calendar.YEAR), now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH));
        dp.getDatePicker().setMinDate(System.currentTimeMillis());
        dp.show();
    }

    // CHANGE updateDateTimeDisplay to update stateHolder
    private void updateDateTimeDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault());
        stateHolder.setDateTimeLabel(sdf.format(selectedDateTime.getTime()));  // was: binding.tvSelectedDateTime
        dateTimeSelected = true;
    }

    // Location launcher results — update stateHolder instead of binding
    // In originPickerLauncher result:
    //   stateHolder.setOrigin(addr);   // was: binding.etOrigin.setText(addr)
    // In destPickerLauncher result:
    //   stateHolder.setDestination(addr);   // was: binding.etDestination.setText(addr)

    // CHANGE validateAndPost to read from stateHolder instead of binding
    public void validateAndPost() {
        String origin      = stateHolder.state.getOriginAddress();
        String destination = stateHolder.state.getDestAddress();
        String seatsStr    = stateHolder.state.getSeats();
        String vehicle     = stateHolder.state.getVehicle();
        String notes       = stateHolder.state.getNotes();
        // ... rest of validation logic UNCHANGED, just reading from state not binding
    }

    @Override
    public void onLoading(boolean isLoading) {
        stateHolder.setLoading(isLoading);   // was: binding.progressBar + binding.btnPost
    }

    // onAdPosted, onAdDeleted, onError — UNCHANGED
}


// ═══════════════════════════════════════════════════════════════════════════════
// LocationPickerActivity.java  — CHANGES SUMMARY
// ═══════════════════════════════════════════════════════════════════════════════

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    // REMOVE: all findViewById calls (etSearch, etPlaceName, btnConfirm, btnSearch,
    //         btnZoomIn, btnZoomOut, btnMyLocation)
    // KEEP: mapView, googleMap, fusedLocation, executor, mainHandler,
    //       pickedLat, pickedLng, verificationId, locationLauncher
    private LocationPickerStateHolder stateHolder;   // ADD

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // REMOVE: setContentView(R.layout.activity_location_picker)
        // REMOVE: all setSupportActionBar, all setOnClickListeners

        stateHolder  = new LocationPickerStateHolder();
        fusedLocation = LocationServices.getFusedLocationProviderClient(this);
        String title  = getIntent().getStringExtra(EXTRA_TITLE);
        LocationPickerBridgeKt.setupComposeUI(this, stateHolder,
                title != null ? title : "Pick Location");   // ADD

        // mapView is now bound via bindMapView() called from bridge's onMapReady
    }

    // ADD — bridge gives us the MapView, we call getMapAsync on it
    public void bindMapView(com.google.android.gms.maps.MapView mv) {
        this.mapView = mv;
        mv.getMapAsync(this);
    }

    // ADD — called by bridge zoom in button
    public void handleZoomIn() {
        if (googleMap != null) googleMap.animateCamera(CameraUpdateFactory.zoomIn());
    }

    // ADD
    public void handleZoomOut() {
        if (googleMap != null) googleMap.animateCamera(CameraUpdateFactory.zoomOut());
    }

    // ADD
    public void handleMyLocation() { requestLocationAndJump(); }

    // ADD
    public void handleSearch(String query) {
        searchLocation(query);
        // hide keyboard still works via existing hideKeyboard()
    }

    // ADD
    public void handleConfirm(String finalName) {
        if (finalName.isEmpty()) {
            Toast.makeText(this, "Please enter or detect a place name", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent result = new Intent();
        result.putExtra(RESULT_LAT,     pickedLat);
        result.putExtra(RESULT_LNG,     pickedLng);
        result.putExtra(RESULT_ADDRESS, finalName);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_POSITION, DEFAULT_ZOOM));
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);

        googleMap.setOnCameraIdleListener(() -> {
            LatLng center = googleMap.getCameraPosition().target;
            pickedLat = center.latitude;
            pickedLng = center.longitude;
            reverseGeocode(center);
        });

        googleMap.setOnCameraMoveStartedListener(reason -> {
            stateHolder.setPlaceNameHint("Detecting…");       // was: etPlaceName.setHint
            stateHolder.setConfirmEnabled(false);             // was: btnConfirm.setEnabled(false)
        });
    }

    // In reverseGeocode(), replace:
    //   etPlaceName.setText(detected)    →  stateHolder.setPlaceName(detected)
    //   etPlaceName.setHint(...)         →  stateHolder.setPlaceNameHint(...)
    //   btnConfirm.setEnabled(true)      →  stateHolder.setConfirmEnabled(true)
    //
    // All other geocoder/location logic UNCHANGED.


    // ── MainActivity.java  — CHANGES SUMMARY ──────────────────────────────────
    // REMOVE: private ActivityMainBinding binding;
    // ADD:    private MainStateHolder stateHolder;
    //
    // In onCreate:
    //   REMOVE: binding = ActivityMainBinding.inflate(...)
    //   REMOVE: setContentView(binding.getRoot())
    //   REMOVE: binding.bottomNav.setOnItemSelectedListener(...)
    //   ADD:    stateHolder = new MainStateHolder();
    //   ADD:    MainBridgeKt.setupComposeUI(this, stateHolder, R.id.fragmentContainer);
    //
    // ADD:
    //   public void onTabSelected(int index) {
    //       stateHolder.setSelectedTab(index);
    //       switch(index) {
    //           case 0: loadFragment(new MapFragment()); break;
    //           case 1: loadFragment(new ListFragment()); break;
    //           case 2: loadFragment(new InboxFragment()); break;
    //           case 3: startActivity(new Intent(this, ProfileActivity.class)); break;
    //       }
    //   }
    //
    //   private void loadFragment(Fragment fragment) {  // UNCHANGED }
    //   private void handleIntent(Intent intent) { ... // update stateHolder.setSelectedTab(1) instead of binding.bottomNav.setSelectedItemId }
}
