package org.adaptlab.chpir.android.participanttracker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.adaptlab.chpir.android.participanttracker.Receivers.InstrumentListReceiver;
import org.adaptlab.chpir.android.participanttracker.Receivers.ReceivedInstrumentDetails;
import org.adaptlab.chpir.android.participanttracker.models.Participant;
import org.adaptlab.chpir.android.participanttracker.models.ParticipantProperty;
import org.adaptlab.chpir.android.participanttracker.models.Property;
import org.adaptlab.chpir.android.participanttracker.models.Relationship;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.json.JSONException;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParticipantDetailFragment extends Fragment {
    private static final String TAG = "ParticipantDetailFragment";
    public final static String EXTRA_PARTICIPANT_ID = 
            "org.adaptlab.chpir.participanttracker.participantdetailfragment.participant_id";
    public final static String SURVEY_PACKAGE_NAME =
            "org.adaptlab.chpir.android.survey";
    public final static String EXTRA_PARTICIPANT_METADATA =
            SURVEY_PACKAGE_NAME + ".metadata";
    private final static int UPDATE_PARTICIPANT = 0;
    
    private Participant mParticipant;
    private static  Participant sParticipant;
    private static String sParticipantMetadata;
    private static String sParticipantType;
    private LinearLayout mParticipantPropertiesContainer;
    private static Activity sActivity;
    private Map<ParticipantProperty, TextView> mParticipantPropertyLabels;
    private Map<Relationship, Button> mRelationshipButtons;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sActivity = getActivity();
        
        if (savedInstanceState != null) {
            mParticipant = Participant.findById(savedInstanceState.getLong(EXTRA_PARTICIPANT_ID));
        } else {
            Long participantId = getActivity().getIntent().getLongExtra(EXTRA_PARTICIPANT_ID, -1);
            if (participantId == -1) return;

            mParticipant = Participant.findById(participantId);
            sParticipant = Participant.findById(participantId);
        }
        
        try {
            sParticipantMetadata = mParticipant.getMetadata();
        } catch (JSONException e) {
            Log.e(TAG, "Could not parse participant metadata for " + mParticipant.getId());
        }
        
        sParticipantType = mParticipant.getParticipantType().getNonLocalizedLabel();
        
        getActivity().setTitle(mParticipant.getLabel());
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View v = inflater.inflate(R.layout.fragment_participant_detail, parent, false);
        mParticipantPropertiesContainer = (LinearLayout) v.findViewById(R.id.participant_properties_container);
        refreshView();
        return v;
    }
    

    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.participant_detail, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_new:
                newSurvey();
                return true;
            case R.id.action_edit_participant:
                Intent i = new Intent(getActivity(), NewParticipantActivity.class);
                i.putExtra(NewParticipantFragment.EXTRA_PARTICIPANT_ID, mParticipant.getId());
                i.putExtra(NewParticipantFragment.EXTRA_PARTICIPANT_TYPE_ID, mParticipant.getParticipantType().getId());
                startActivityForResult(i, UPDATE_PARTICIPANT);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == UPDATE_PARTICIPANT) {
            if (resultCode == Activity.RESULT_OK) {
                
                refreshView();
                
            }
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        sActivity = getActivity();
    }
    
    public static void displayInstrumentPicker(final List<ReceivedInstrumentDetails> instrumentDetails) {
        AlertDialog.Builder builder = new AlertDialog.Builder(sActivity);
        builder.setTitle(sActivity.getString(R.string.choose_instrument));
        
        final List<String> instrumentTitleList = new ArrayList<String>();
        final List<Long> instrumentIdList = new ArrayList<Long>();
        int participantAge = 0;
        participantAge = getParticipantAge(participantAge);

        for (ReceivedInstrumentDetails d : instrumentDetails) {
            String startAge = d.getParticipantStartAge();
            String endAge = d.getParticipantEndAge();
            if (!TextUtils.isEmpty(startAge) && !TextUtils.isEmpty(endAge)) {
                if (!(participantAge >= Integer.parseInt(startAge) && participantAge < Integer.parseInt(endAge))) {
                    if (BuildConfig.DEBUG) Log.i(TAG, d.getTitle()+ " Fails Age Rule");
                    continue;
                }
            }
            if (!d.getParticipantType().equals("") && !d.getParticipantType().equals(sParticipantType)) continue;
            instrumentTitleList.add(d.getTitle());
            instrumentIdList.add(d.getId());
        }
        
        builder.setSingleChoiceItems(instrumentTitleList.toArray(new String[instrumentTitleList.size()]), -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent();
                i.setAction(InstrumentListReceiver.START_SURVEY);
                i.putExtra(InstrumentListReceiver.START_SURVEY_INSTRUMENT_ID, instrumentIdList.get(which));
                i.putExtra(EXTRA_PARTICIPANT_METADATA, sParticipantMetadata);
                sActivity.sendBroadcast(i);
                dialog.cancel();
                sActivity.finish();
            }
        }); 
        
        builder.show();
    }

    private static int getParticipantAge(int participantAge) {
        Property DofBProperty = getBirthDateProperty();
        ParticipantProperty DofBPropertyValue;
        if (DofBProperty != null && sParticipant.hasParticipantProperty(DofBProperty)) {
            DofBPropertyValue = sParticipant.getParticipantProperty(DofBProperty);
            if (DofBPropertyValue.getValue() != null && !DofBPropertyValue.getValue().equals("")) {
                String[] dateList = DofBPropertyValue.getValue().split("-");
                if (dateList.length == 3) {
                    GregorianCalendar birthDate = new GregorianCalendar(Integer.parseInt(dateList[2]), Integer.parseInt(dateList[0]), Integer.parseInt(dateList[1]));
                    Period age = new Period(new DateTime(birthDate.getTime()).toInstant(), new DateTime().toInstant(), PeriodType.yearMonthDay());
                    participantAge = age.getYears();

                }
            }
        }
        return participantAge;
    }

    private static Property getBirthDateProperty() {
        for (Property property : Property.getAll()) {
            if (property.getLabel().equals("Child DOB")) {
                return property;
            }
        }
        return null;
    }
    
    private void newSurvey() {
            Intent i = new Intent();
            i.setAction(InstrumentListReceiver.GET_INSTRUMENT_LIST);
            getActivity().getApplicationContext().sendBroadcast(i);
    }
    
    /*
     * Return the text view for the value so that it may be updated later.
     */
    private TextView addKeyValueLabel(String key, String value) {
        addHeader(key);
        
        TextView textView = new TextView(getActivity());
        textView.setText(styleValueLabel(value));
        mParticipantPropertiesContainer.addView(textView);
        return textView;
    }
    
    private SpannableString styleValueLabel(String value) {
        SpannableString spanString = new SpannableString(value);
        spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
        return spanString;
    }
    
    private void addHeader(String label) {
        TextView textView = new TextView(getActivity());
        textView.setTextAppearance(getActivity(), R.style.sectionHeader);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 25, 0, 0);
        textView.setLayoutParams(layoutParams);
        mParticipantPropertiesContainer.addView(textView);
        textView.setText(label);                
    }
    
    /*
     * Create the necessary mappings from participant relationships and properties
     * to their corresponding UI elements.
     * 
     */
    @SuppressLint("LongLogTag")
    private void refreshView() {
        mParticipantPropertyLabels = new HashMap<ParticipantProperty, TextView>();
        mRelationshipButtons = new HashMap<Relationship, Button>();
        
        mParticipantPropertiesContainer.removeAllViews();
        
        for (ParticipantProperty participantProperty : mParticipant.getParticipantProperties()) {
            String birthDay = participantProperty.getValue().replaceAll("\\s+", "");
            Integer[] dateNums = getDateIntegers(birthDay);
            if (participantProperty.getProperty().getTypeOf().equals(Property.PropertyType.DATE)
                    && dateNums != null && dateNums.length == 3) {
                GregorianCalendar dateOfBirth = new GregorianCalendar(dateNums[2], dateNums[0] - 1, dateNums[1]);
                Date birthDate = dateOfBirth.getTime();
                DateFormat df = DateFormat.getDateInstance();
                mParticipantPropertyLabels.put(participantProperty,
                        addKeyValueLabel(participantProperty.getProperty().getLabel(), df.format(birthDate)));

                Period age = new Period(new DateTime(birthDate).toInstant(), new DateTime().toInstant(), PeriodType.yearMonthDay());
                int ageInMonths = age.getYears() * 12 + age.getMonths();
                String ageString = age.getYears() + " " + getString(R.string.years) + " " + age.getMonths() + " "
                        + getString(R.string.months) + " " + age.getDays() + " " + getString(R.string.days) + " "
                        + getString(R.string.or) + " " + ageInMonths + " " + getString(R.string.months) + " "
                        + age.getDays() + " " + getString(R.string.days);

                addKeyValueLabel(getString(R.string.age), ageString);
            } else {
                mParticipantPropertyLabels.put(participantProperty,
                        addKeyValueLabel(participantProperty.getProperty().getLabel(), participantProperty.getValue()));
            }
        }

        for (final Relationship relationship : mParticipant.getRelationships()) {
            addHeader(relationship.getRelationshipType().getLabel());
            
            Button button = new Button(getActivity());
            button.setText(relationship.getParticipantRelated().getLabel());
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent i = new Intent(getActivity(), ParticipantDetailActivity.class);
                    i.putExtra(ParticipantDetailFragment.EXTRA_PARTICIPANT_ID, relationship.getParticipantRelated().getId());
                    startActivity(i);
                }
            });
            mParticipantPropertiesContainer.addView(button);
            mRelationshipButtons.put(relationship, button);
        }
        
        addKeyValueLabel("UUID", mParticipant.getUUID());
    }

    private Integer[] getDateIntegers(String date) {
        Integer[] dateNumbers = new Integer[3];
        int index = 0;
        for (String s : date.split("-")) {
            try {
                dateNumbers[index] = Integer.parseInt(s);
                index++;
            } catch (NumberFormatException e) {
                return null;
            } catch (NullPointerException e) {
                return null;
            }
        }
        return dateNumbers;
    }
}
