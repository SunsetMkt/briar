package org.briarproject.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import org.briarproject.R;
import org.briarproject.android.controller.NavDrawerController;
import org.briarproject.android.controller.TransportStateListener;
import org.briarproject.android.controller.handler.UiResultHandler;
import org.briarproject.android.fragment.BaseFragment;
import org.briarproject.android.util.CustomAnimations;
import org.briarproject.api.TransportId;
import org.briarproject.api.identity.LocalAuthor;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

import static android.support.v4.view.GravityCompat.START;
import static android.support.v4.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED;
import static android.support.v4.widget.DrawerLayout.LOCK_MODE_UNLOCKED;
import static android.view.View.INVISIBLE;

public class NavDrawerActivity extends BriarFragmentActivity implements
		BaseFragment.BaseFragmentListener, TransportStateListener {

	public final static String PREF_SEEN_WELCOME_MESSAGE = "welcome_message";

	public static final String INTENT_CONTACTS = "intent_contacts";

	private static final String KEY_CURRENT_FRAGMENT_ID = "key_current_id";

	private static final Logger LOG =
			Logger.getLogger(NavDrawerActivity.class.getName());

	private ActionBarDrawerToggle drawerToggle;

	@Inject
	protected NavDrawerController controller;

	private Toolbar toolbar;
	private DrawerLayout drawerLayout;
	private TextView progressTitle;
	private ViewGroup progressViewGroup;

	private List<Transport> transports;
	private BaseAdapter transportsAdapter;
	private int currentFragmentId = R.id.nav_btn_contacts;

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		exitIfStartupFailed(intent);
		checkAuthorHandle(intent);
		clearBackStack();
		if (intent.getBooleanExtra(INTENT_CONTACTS, false))
			startFragment(activityComponent.newContactListFragment());
	}

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}

	@SuppressWarnings("ConstantConditions")
	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		exitIfStartupFailed(getIntent());
		setContentView(R.layout.activity_nav_drawer);

		toolbar = (Toolbar) findViewById(R.id.toolbar);
		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		GridView transportsView = (GridView) findViewById(R.id.transportsView);
		progressTitle = (TextView) findViewById(R.id.title_progress_bar);
		progressViewGroup = (ViewGroup) findViewById(R.id.container_progress);

		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
				R.string.nav_drawer_open_description,
				R.string.nav_drawer_close_description);
		drawerLayout.setDrawerListener(drawerToggle);
		if (state == null) {
			startFragment(activityComponent.newContactListFragment());
		} else {
			currentFragmentId = state.getInt(KEY_CURRENT_FRAGMENT_ID);
			loadCurrentFragment();
		}
		checkAuthorHandle(getIntent());

		initializeTransports(getLayoutInflater());
		transportsView.setAdapter(transportsAdapter);

		welcomeMessageCheck();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_CURRENT_FRAGMENT_ID, currentFragmentId);
	}

	private void welcomeMessageCheck() {
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		if (!prefs.getBoolean(PREF_SEEN_WELCOME_MESSAGE, false)) {
			showMessageDialog(R.string.dialog_title_welcome,
					R.string.dialog_welcome_message);
			prefs.edit().putBoolean(PREF_SEEN_WELCOME_MESSAGE, true).apply();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		updateTransports();
	}

	private void checkAuthorHandle(Intent intent) {
		long handle = intent.getLongExtra(KEY_LOCAL_AUTHOR_HANDLE, -1);
		if (handle != -1) {
			LocalAuthor a = controller.removeAuthorHandle(handle);
			// The activity was launched from the setup wizard
			if (a != null) {
				showLoadingScreen(true, R.string.progress_title_please_wait);
				storeLocalAuthor(a);
			}
		}
	}

	private void exitIfStartupFailed(Intent intent) {
		if (intent.getBooleanExtra(KEY_STARTUP_FAILED, false)) {
			finish();
			LOG.info("Exiting");
			System.exit(0);
		}
	}

	private void storeLocalAuthor(LocalAuthor a) {
		controller.storeLocalAuthor(a, new UiResultHandler<Void>(this) {
			@Override
			public void onResultUi(Void result) {
				hideLoadingScreen();
			}
		});
	}

	private void loadCurrentFragment() {
		switch (currentFragmentId) {
			case R.id.nav_btn_contacts:
				startFragment(activityComponent.newContactListFragment());
				break;
			case R.id.nav_btn_settings:
				startActivity(new Intent(this, SettingsActivity.class));
				break;
			case R.id.nav_btn_signout:
				signOut();
				break;
		}
	}

	public void onNavigationClick(View view) {
		drawerLayout.closeDrawer(START);
		clearBackStack();
		currentFragmentId = view.getId();
		loadCurrentFragment();
	}


	@Override
	public void onBackPressed() {
		if (getSupportFragmentManager().getBackStackEntryCount() == 0
				&& drawerLayout.isDrawerOpen(START)) {
			drawerLayout.closeDrawer(START);
			return;
		}
		super.onBackPressed();
	}

	@Override
	public void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		drawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected void signOut() {
		showLoadingScreen(true, R.string.progress_title_logout);
		super.signOut();
	}

	@Override
	public void showLoadingScreen(boolean isBlocking, int stringId) {
		if (isBlocking) {
			// Disable navigation drawer slide to open
			drawerLayout.setDrawerLockMode(LOCK_MODE_LOCKED_CLOSED);
			CustomAnimations.animateHeight(toolbar, false, 250);
		}
		progressTitle.setText(stringId);
		progressViewGroup.setVisibility(View.VISIBLE);
	}

	@Override
	public void hideLoadingScreen() {
		drawerLayout.setDrawerLockMode(LOCK_MODE_UNLOCKED);
		CustomAnimations.animateHeight(toolbar, true, 250);
		progressViewGroup.setVisibility(INVISIBLE);
	}

	private void initializeTransports(final LayoutInflater inflater) {
		transports = new ArrayList<>(3);

		Transport tor = new Transport();
		tor.id = new TransportId("tor");
		tor.enabled = controller.isTransportRunning(tor.id);
		tor.iconId = R.drawable.transport_tor;
		tor.textId = R.string.transport_tor;
		transports.add(tor);

		Transport bt = new Transport();
		bt.id = new TransportId("bt");
		bt.enabled = controller.isTransportRunning(bt.id);
		bt.iconId = R.drawable.transport_bt;
		bt.textId = R.string.transport_bt;
		transports.add(bt);

		Transport lan = new Transport();
		lan.id = new TransportId("lan");
		lan.enabled = controller.isTransportRunning(lan.id);
		lan.iconId = R.drawable.transport_lan;
		lan.textId = R.string.transport_lan;
		transports.add(lan);

		transportsAdapter = new BaseAdapter() {
			@Override
			public int getCount() {
				return transports.size();
			}

			@Override
			public Transport getItem(int position) {
				return transports.get(position);
			}

			@Override
			public long getItemId(int position) {
				return 0;
			}

			@Override
			public View getView(int position, View convertView,
					ViewGroup parent) {
				ViewGroup view = (ViewGroup) inflater.inflate(
						R.layout.list_item_transport, parent, false);

				Transport t = getItem(position);
				Resources r = getResources();

				int c;
				if (t.enabled) {
					c = r.getColor(R.color.briar_green_light);
				} else {
					c = r.getColor(android.R.color.tertiary_text_light);
				}

				ImageView icon = (ImageView) view.findViewById(R.id.imageView);
				icon.setImageDrawable(r.getDrawable(t.iconId));
				icon.setColorFilter(c);

				TextView text = (TextView) view.findViewById(R.id.textView);
				text.setText(getString(t.textId));

				return view;
			}
		};
	}

	private void setTransport(final TransportId id, final boolean enabled) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (transports == null || transportsAdapter == null) return;
				for (Transport t : transports) {
					if (t.id.equals(id)) {
						t.enabled = enabled;
						transportsAdapter.notifyDataSetChanged();
						break;
					}
				}
			}
		});
	}

	private void updateTransports() {
		if (transports == null || transportsAdapter == null) return;
		for (Transport t : transports) {
			t.enabled = controller.isTransportRunning(t.id);
		}
		transportsAdapter.notifyDataSetChanged();
	}

	@Override
	public void stateUpdate(TransportId id, boolean enabled) {
		setTransport(id, enabled);
	}

	private static class Transport {

		private TransportId id;
		private boolean enabled;
		private int iconId;
		private int textId;
	}
}
