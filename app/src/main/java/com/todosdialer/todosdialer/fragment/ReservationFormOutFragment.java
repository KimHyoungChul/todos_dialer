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

public class ReservationFormOutFragment extends Fragment implements DatePickerDialog.OnDateSetListener,
        TimePickerDialog.OnTimeSetListener {
    private static final String FORMAT_DATE = "yyyy-MM-dd";
    private static final String FORMAT_TIME = "HH:mm";

    private TextView mTextOutDate;
    private TextView mTextOutTime;
    private TextView mTextOutAirport;
    private ProgressBar mProgressBar;

    private String mSelectedOutDate;
    private String mSelectedOutTime;
    private AppCode mSelectedOutAirport;

    private AppCodeGroup mSelectedBasicNation;

    private OnSelectionListener mOnSelectionListener;

    public static ReservationFormOutFragment newInstance(AppCodeGroup basicNation) {

        Bundle args = new Bundle();

        ReservationFormOutFragment fragment = new ReservationFormOutFragment();
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSelectedOutDate = null;
        mSelectedOutTime = null;
        mSelectedOutAirport = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_reservation_form_out, container, false);
        mTextOutDate = rootView.findViewById(R.id.text_out_date);
        mTextOutTime = rootView.findViewById(R.id.text_out_time);
        mTextOutAirport = rootView.findViewById(R.id.text_out_air_port_code_name);
        mProgressBar = rootView.findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);

        mTextOutDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        mTextOutTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        });

        mTextOutAirport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadAirportAppCodes(mSelectedBasicNation);
            }
        });

        rootView.findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAllSelected() && mOnSelectionListener != null) {
                    mOnSelectionListener.onSelectionFinished(mSelectedOutDate, mSelectedOutTime, mSelectedOutAirport);
                }
            }
        });
        return rootView;
    }

    private boolean isAllSelected() {
        if (TextUtils.isEmpty(mSelectedOutDate)) {
            Toast.makeText(getContext(), R.string.hint_out_date, Toast.LENGTH_SHORT).show();
            return false;
        } else if (TextUtils.isEmpty(mSelectedOutTime)) {
            Toast.makeText(getContext(), R.string.hint_out_time, Toast.LENGTH_SHORT).show();
            return false;
        } else if (mSelectedOutAirport == null) {
            Toast.makeText(getContext(), R.string.hint_out_air_port_code_name, Toast.LENGTH_SHORT).show();
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
        mSelectedOutDate = Utils.formatTime(calendar.getTime(), FORMAT_DATE);
        mTextOutDate.setText(mSelectedOutDate);
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
        mSelectedOutTime = Utils.formatTime(calendar.getTime(), FORMAT_TIME);
        mTextOutTime.setText(mSelectedOutTime);
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
                mSelectedOutAirport = item;
                mTextOutAirport.setText(item.codeName);
            }
        });
        fragment.show(getChildFragmentManager(), "mSelectedOutNation");
    }

    public interface OnSelectionListener {
        void onSelectionFinished(String outDate, String outTime, AppCode outAirport);
    }
}
