package com.todosdialer.todosdialer.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.api.ApiCallback;
import com.todosdialer.todosdialer.api.Client;
import com.todosdialer.todosdialer.api.body.AppCodeBody;
import com.todosdialer.todosdialer.api.body.CodeGroupBody;
import com.todosdialer.todosdialer.api.response.LoadAppCodeResponse;
import com.todosdialer.todosdialer.api.response.LoadCodeGroupResponse;
import com.todosdialer.todosdialer.manager.RetrofitManager;
import com.todosdialer.todosdialer.model.AppCode;
import com.todosdialer.todosdialer.model.AppCodeGroup;

import java.util.ArrayList;

public class ReservationFormBasicFragment extends Fragment {
    private TextView mTextBasicNation;
    private TextView mTextBasicNationTelecom;
    private TextView mTextOutNation;
    private ProgressBar mProgressBar;

    private AppCodeGroup mSelectedBasicNation;
    private AppCode mSelectedBasicNationTelecom;
    private AppCodeGroup mSelectedOutNation;

    private OnSelectionListener mOnSelectionListener;

    public static ReservationFormBasicFragment newInstance() {
        Bundle args = new Bundle();
        ReservationFormBasicFragment fragment = new ReservationFormBasicFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSelectedBasicNation = null;
        mSelectedBasicNationTelecom = null;
        mSelectedOutNation = null;
    }

    public void setOnSelectionListener(OnSelectionListener listener) {
        mOnSelectionListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_reservation_form_basic, container, false);
        mTextBasicNation = rootView.findViewById(R.id.text_basic_nation);
        mTextBasicNationTelecom = rootView.findViewById(R.id.text_basic_telecom);
        mTextOutNation = rootView.findViewById(R.id.text_out_nation);
        mProgressBar = rootView.findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);

        mTextBasicNation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadNationAppCodeGroups(true);
            }
        });

        mTextBasicNationTelecom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadTelecomAppCodes(mSelectedBasicNation);
            }
        });

        mTextOutNation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadNationAppCodeGroups(false);
            }
        });

        rootView.findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAllSelected() && mOnSelectionListener != null) {
                    mOnSelectionListener.onSelectionFinished(mSelectedBasicNation, mSelectedBasicNationTelecom, mSelectedOutNation);
                }
            }
        });

        return rootView;
    }

    private void loadNationAppCodeGroups(final boolean isBasic) {
        mProgressBar.setVisibility(View.VISIBLE);
        RetrofitManager.retrofit(getContext()).create(Client.Api.class)
                .loadAppCodeGroup(new CodeGroupBody(AppCodeGroup.GROUP_NATION))
                .enqueue(new ApiCallback<LoadCodeGroupResponse>() {
                    @Override
                    public void onSuccess(LoadCodeGroupResponse response) {
                        mProgressBar.setVisibility(View.GONE);
                        if (response.isSuccess()) {
                            if (isBasic) {
                                showSelectionBasicNationFragment(response.result);
                            } else {
                                showSelectionOutNationFragment(response.result);
                            }
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


    private void loadTelecomAppCodes(AppCodeGroup mSelectedBasicNation) {
        mProgressBar.setVisibility(View.VISIBLE);

        if (mSelectedBasicNation == null) {
            Toast.makeText(getContext(), R.string.hint_basic_nation_code_name, Toast.LENGTH_SHORT).show();
            return;
        }

        RetrofitManager.retrofit(getContext()).create(Client.Api.class)
                .loadAppCode(new AppCodeBody(AppCodeGroup.GROUP_NATION, mSelectedBasicNation.code, AppCodeGroup.GROUP_TELECOM))
                .enqueue(new ApiCallback<LoadAppCodeResponse>() {
                    @Override
                    public void onSuccess(LoadAppCodeResponse response) {
                        mProgressBar.setVisibility(View.GONE);
                        if (response.isSuccess()) {
                            showSelectionTelecomFragment(response.result);
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

    private boolean isAllSelected() {
        if (mSelectedBasicNation == null) {
            Toast.makeText(getContext(), R.string.hint_basic_nation_code_name, Toast.LENGTH_SHORT).show();
            return false;
        } else if (mSelectedBasicNationTelecom == null) {
            Toast.makeText(getContext(), R.string.hint_basic_telecom_code_name, Toast.LENGTH_SHORT).show();
            return false;
        } else if (mSelectedOutNation == null) {
            Toast.makeText(getContext(), R.string.hint_out_nation_code_name, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showSelectionBasicNationFragment(final ArrayList<AppCodeGroup> codeGroups) {
        SelectionDialogFragment<AppCodeGroup> fragment = new SelectionDialogFragment<>();
        fragment.setItemInterface(new SelectionDialogFragment.ItemInterface<AppCodeGroup>() {
            @Override
            public ArrayList<AppCodeGroup> getList() {
                return codeGroups;
            }

            @Override
            public String convertString(AppCodeGroup item) {
                return item.codeName;
            }
        });
        fragment.setOnItemClickListener(new SelectionDialogFragment.OnItemClickListener<AppCodeGroup>() {
            @Override
            public void onItemClicked(AppCodeGroup item) {
                mSelectedBasicNation = item;
                mTextBasicNation.setText(item.codeName);
            }
        });
        fragment.show(getChildFragmentManager(), "mSelectedBasicNation");
    }

    private void showSelectionOutNationFragment(final ArrayList<AppCodeGroup> codeGroups) {
        SelectionDialogFragment<AppCodeGroup> fragment = new SelectionDialogFragment<>();
        fragment.setItemInterface(new SelectionDialogFragment.ItemInterface<AppCodeGroup>() {
            @Override
            public ArrayList<AppCodeGroup> getList() {
                return codeGroups;
            }

            @Override
            public String convertString(AppCodeGroup item) {
                return item.codeName;
            }
        });
        fragment.setOnItemClickListener(new SelectionDialogFragment.OnItemClickListener<AppCodeGroup>() {
            @Override
            public void onItemClicked(AppCodeGroup item) {
                mSelectedOutNation = item;
                mTextOutNation.setText(item.codeName);
            }
        });
        fragment.show(getChildFragmentManager(), "mSelectedOutNation");
    }

    private void showSelectionTelecomFragment(final ArrayList<AppCode> codeGroups) {
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
                mSelectedBasicNationTelecom = item;
                mTextBasicNationTelecom.setText(item.codeName);
            }
        });
        fragment.show(getChildFragmentManager(), "mSelectedOutNation");
    }

    public interface OnSelectionListener {
        void onSelectionFinished(AppCodeGroup basicNation, AppCode basicTelecom, AppCodeGroup outNation);
    }
}
