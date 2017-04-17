package com.newsblur.fragment;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.newsblur.R;
import com.newsblur.view.StateToggleButton;
import com.newsblur.view.StateToggleButton.StateChangedListener;
import com.newsblur.util.StateFilter;

public class FeedIntelligenceSelectorFragment extends Fragment implements StateChangedListener {
	
	public static final String FRAGMENT_TAG = "feedIntelligenceSelector";
	private StateToggleButton button;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_intelligenceselector, null);
		button = (StateToggleButton) v.findViewById(R.id.fragment_intelligence_statebutton);
		button.setStateListener(this);
		return v;
	}

	@Override
	public void changedState(StateFilter state) {
		((StateChangedListener) getActivity()).changedState(state);
	}
	
	public void setState(StateFilter state) {
		button.setState(state);
	}

}