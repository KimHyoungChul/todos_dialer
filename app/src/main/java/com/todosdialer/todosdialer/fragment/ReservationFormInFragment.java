package com.todosdialer.todosdialer.fragment;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.api.ApiCallback;
import com.todosdialer.todosdialer.api.Client;
import com.todosdialer.todosdialer.api.body.AppCodeBody;
import com.todosdialer.todosdialer.api.response.LoadAppCodeResponse;
import com.todosdialer.todosdialer.manager.RetrofitManager;
import com.todosdialer.todosdialer.model.AppCode;
import com.todosdialer.todosdialer.model.AppCodeGroup;
import com.todosdialer.todosdialer.util.Utils;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.util.ArrayList;
import java.util.Calendar;

public class ReservationFormInFragment extends Fragment implements DatePickerDialog.OnDateSetListener,
        TimePickerDialog.OnTimeSetListener {
    private static final String FORMAT_DATE = "yyyy-MM-dd";
    private static final String FORMAT_TIME = "HH:mm";

    private TextView mTextInDate;
    private TextView mTextInTime;
    private TextView mTextInAirport;
    private ProgressBar mProgressBar;

    private String mSelectedInDate;
    private String mSelectedInTime;
    private AppCode mSelectedInAirport;

    private AppCodeGroup mSelectedBasicNation;
    private OnSelectionListener mOnSelectionListener;

    public static ReservationFormInFragment newInstance(AppCodeGroup basicNation) {

        Bundle args = new Bundle();

        ReservationFormInFragment fragment = new ReservationFormInFragment();
        fragment.setArguments(args);
        fragment.setSelectedBasicNation(basicNation);
        return fragment;
    }

    public void setSelectedBasicNation(AppCodeGroup basicNation) {
        mSelectedBasicNation = basicNation;
    }

    public void setOnSelectionListener(OnSelectionListener listener) {
        mOnSelectionListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_reservation_form_in, container, false);
        mTextInDate = rootView.findViewById(R.id.text_in_date);
        mTextInTime = rootView.findViewById(R.id.text_in_time);
        mTextInAirport = rootView.findViewById(R.id.text_in_air_port_code_name);
        mProgressBar = rootView.findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);

        mTextInDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        mTextInTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        });

        mTextInAirport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadAirportAppCodes(mSelectedBasicNation);
            }
        });

        rootView.findViewById(R.id.btn_register_reservation).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAllSelected() && mOnSelectionListener != null) {
                    mOnSelectionListener.onSelectionFinished(mSelectedInDate, mSelectedInTime, mSelectedInAirport);
                }
            }
        });
        return rootView;
    }

    private boolean isAllSelected() {
        if (TextUtils.isEmpty(mSelectedInDate)) {
            Toast.makeText(getContext(), R.string.hint_in_date, Toast.LENGTH_SHORT).show();
            return false;
        } else if (TextUtils.isEmpty(mSelectedInTime)) {
            Toast.makeText(getContext(), R.string.hint_in_time, Toast.LENGTH_SHORT).show();
            return false;
        } else if (mSelectedInAirport == null) {
            Toast.makeText(getContext(), R.string.hint_in_air_port_code_name, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showDatePickerDialog() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog dialog = DatePickerDialog.newInstance(
                this,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show(getActivity().getFragmentManager(), "DatePickerDialog");
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        mSelectedInDate = Utils.formatTime(calendar.getTime(), FORMAT_DATE);
        mTextInDate.setText(mSelectedInDate);
    }

    private void showTimePickerDialog() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(
                getActivity(), this,
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
        );
        dialog.show();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        mSelectedInTime = Utils.formatTime(calendar.getTime(), FORMAT_TIME);
        mTextInTime.setText(mSelectedInTime);
    }

    private void loadAirportAppCodes(AppCodeGroup mSelectedBasicNation) {
        mProgressBar.setVisibility(View.VISIBLE);

        if (mSelectedBasicNation == null) {
            Toast.makeText(getContext(), R.string.hint_basic_nation_code_name, Toast.LENGTH_SHORT).show();
            return;
        }

        RetrofitManager.retrofit(getContext()).create(Client.Api.class)
                .loadAppCode(new AppCodeBody(AppCodeGroup.GROUP_NATION, mSelectedBasicNation.code, AppCodeGroup.GROUP_AIRPORT))
                .enqueue(new ApiCallback<LoadAppCodeResponse>() {
                    @Override
                    public void onSuccess(LoadAppCodeResponse response) {
                        mProgressBar.setVisibility(View.GONE);
                        if (response.isSuccess()) {
                            showSelectionAirportFragment(response.result);
                        } else {
                            Toast.makeText(getContext(), response.message, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFail(int error, String msg) {
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                        mProgressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void showSelectionAirportFragment(final ArrayList<AppCode> codeGroups) {
        SelectionDialogFragment<AppCode> fragment = new SelectionDialogFragment<>();
        fragment.setItemInterface(new SelectionDialogFragment.ItemInterface<AppCode>() {
            @Override
            public ArrayList<AppCode> getList() {
                return codeGroups;
            }

            @Override
            public String convertString(AppCode item) {
                return item.codeName;
            }
        });
        fragment.setOnItemClickListener(new SelectionDialogFragment.OnItemClickListener<AppCode>() {
            @Override
            public void onItemClicked(AppCode item) {
                mSelectedInAirport = item;
                mTextInAirport.setText(item.codeName);
            }
        });
        fragment.show(getChildFragmentManager(), "mSelectedOutNation");
    }


    public interface OnSelectionListener {
        void onSelectionFinished(String inDate, String inTime, AppCode inAirport);
    }
}
