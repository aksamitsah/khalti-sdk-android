package com.khalti.form.Wallet;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.khalti.R;
import com.khalti.SmsListener;
import com.khalti.carbonX.widget.Button;
import com.khalti.carbonX.widget.ExpandableLayout;
import com.khalti.carbonX.widget.FrameLayout;
import com.khalti.carbonX.widget.TextInputLayout;
import com.khalti.form.CheckOutActivity;
import com.khalti.utils.DataHolder;
import com.khalti.utils.Event;
import com.khalti.utils.RxBus;
import com.utila.AppPermissionUtil;
import com.utila.EmptyUtil;
import com.utila.NetworkUtil;
import com.utila.NumberUtil;
import com.utila.ResourceUtil;
import com.utila.UserInterfaceUtil;

import fontana.RobotoMediumTextView;
import rx.subscriptions.CompositeSubscription;


public class Wallet extends Fragment implements WalletContract.View {

    private TextInputEditText etMobile, etCode, etPIN;
    private TextInputLayout tilMobile, tilCode, tilPIN;
    private ExpandableLayout elConfirmation;
    private Button btnPay;
    private Dialog progressDialog;

    private FragmentActivity fragmentActivity;
    private WalletContract.Listener listener;
    private CompositeSubscription compositeSubscription;
    private SmsListener smsListener;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.payment_form, container, false);
        fragmentActivity = getActivity();
        listener = new WalletPresenter(this);

        etMobile = mainView.findViewById(R.id.etMobile);
        etCode = mainView.findViewById(R.id.etCode);
        etPIN = mainView.findViewById(R.id.etPIN);
        tilMobile = mainView.findViewById(R.id.tilMobile);
        tilCode = mainView.findViewById(R.id.tilCode);
        tilPIN = mainView.findViewById(R.id.tilPIN);
        btnPay = mainView.findViewById(R.id.btnPay);
        elConfirmation = mainView.findViewById(R.id.elConfirmation);

        listener.setUpLayout();

        compositeSubscription = new CompositeSubscription();
        compositeSubscription.add(RxBus.getInstance().register(Event.class, event -> {
            if (event.getTag().equals("wallet_code")) {
                listener.setConfirmationCode(event);
            }
        }));

        return mainView;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (EmptyUtil.isNotNull(compositeSubscription) && !compositeSubscription.isUnsubscribed()) {
            compositeSubscription.unsubscribe();
        }
        listener.toggleSmsListener(false);
    }

    @Override
    public void setEditTextListener() {
        if (EmptyUtil.isNotNull(etMobile)) {
            etMobile.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    tilMobile.setErrorEnabled(false);
                    if (btnPay.getText().toString().toLowerCase().contains("confirm")) {
                        listener.toggleConfirmationLayout(false);
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });
        }

        if (EmptyUtil.isNotNull(etCode)) {
            etCode.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    tilCode.setErrorEnabled(false);
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });
        }

        if (EmptyUtil.isNotNull(etPIN)) {
            etPIN.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    tilPIN.setErrorEnabled(false);
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });
        }
    }

    @Override
    public void toggleProgressDialog(String action, boolean show) {
        FrameLayout flCircularProgress = (FrameLayout) fragmentActivity.getLayoutInflater().inflate(R.layout.component_circular_progress, null);
        String message = "";
        switch (action) {
            case "init":
                message = ResourceUtil.getString(fragmentActivity, R.string.init_payment);
                break;
            case "confirm":
                message = ResourceUtil.getString(fragmentActivity, R.string.confirming_payment);
                break;
        }

        if (show) {
            progressDialog = UserInterfaceUtil.runProgressDialog(fragmentActivity, message, ResourceUtil.getString(fragmentActivity, R.string.please_wait), flCircularProgress, () -> {
                listener.unSubscribe();
            });
        } else {
            if (EmptyUtil.isNotNull(progressDialog)) {
                progressDialog.dismiss();
            }
        }
    }

    @Override
    public void setEditTextError(String view, String error) {
        switch (view) {
            case "mobile":
                tilMobile.setError(error);
                break;
            case "code":
                tilCode.setError(error);
                break;
            case "pin":
                tilPIN.setError(error);
                break;
        }
    }

    @Override
    public void setButtonText(String text) {
        btnPay.setText(text);
    }

    @Override
    public void setButtonClickListener() {
        btnPay.setOnClickListener(view -> {
            if (btnPay.getText().toString().toLowerCase().contains("confirm")) {
                listener.confirmPayment(NetworkUtil.isNetworkAvailable(fragmentActivity), etCode.getText().toString(), etPIN.getText().toString());
            } else {
                if (AppPermissionUtil.checkAndroidPermission(fragmentActivity, Manifest.permission.RECEIVE_SMS)) {
                    listener.initiatePayment(NetworkUtil.isNetworkAvailable(fragmentActivity), etMobile.getText().toString());
                } else {
                    FrameLayout flPositive = (FrameLayout) fragmentActivity.getLayoutInflater().inflate(R.layout.component_flat_button, null);
                    RobotoMediumTextView tvPositive = flPositive.findViewById(R.id.tvButton);
                    tvPositive.setText(ResourceUtil.getString(fragmentActivity, R.string.allow));

                    FrameLayout flNegative = (FrameLayout) fragmentActivity.getLayoutInflater().inflate(R.layout.component_flat_button, null);
                    RobotoMediumTextView tvNegative = flNegative.findViewById(R.id.tvButton);
                    tvNegative.setText(ResourceUtil.getString(fragmentActivity, R.string.deny));

                    AppPermissionUtil.askPermission(fragmentActivity, Manifest.permission.RECEIVE_SMS, "Please allow permission to receive SMS", flPositive, flNegative, () ->
                            listener.initiatePayment(NetworkUtil.isNetworkAvailable(fragmentActivity), etMobile.getText().toString()));
                }
            }
        });
    }

    @Override
    public void setConfirmationCode(String code) {
        etCode.setText(code);
        etPIN.requestFocus();
    }

    @Override
    public void showNetworkError() {
        UserInterfaceUtil.showSnackBar(fragmentActivity, ((CheckOutActivity) this.fragmentActivity).cdlMain, ResourceUtil.getString(fragmentActivity, R.string.network_error_body),
                false, "", 0, 0, null);
    }

    @Override
    public void showMessageDialog(String title, String message) {
        FrameLayout flButton = (FrameLayout) fragmentActivity.getLayoutInflater().inflate(R.layout.component_flat_button, null);
        RobotoMediumTextView tvButton = flButton.findViewById(R.id.tvButton);
        tvButton.setText(ResourceUtil.getString(fragmentActivity, R.string.got_it));

        UserInterfaceUtil.showInfoDialog(fragmentActivity, title, message, true, true, flButton, new UserInterfaceUtil.DialogAction() {
            @Override
            public void onPositiveAction(Dialog dialog) {
                dialog.dismiss();
            }

            @Override
            public void onNegativeAction(Dialog dialog) {

            }
        });
    }

    @Override
    public void showInteractiveMessageDialog(String title, String message) {
        FrameLayout flPositive = (FrameLayout) fragmentActivity.getLayoutInflater().inflate(R.layout.component_flat_button, null);
        RobotoMediumTextView tvPositive = flPositive.findViewById(R.id.tvButton);
        tvPositive.setText(ResourceUtil.getString(fragmentActivity, R.string.ok));

        FrameLayout flNegative = (FrameLayout) fragmentActivity.getLayoutInflater().inflate(R.layout.component_flat_button, null);
        RobotoMediumTextView tvNegative = flNegative.findViewById(R.id.tvButton);
        tvNegative.setText(ResourceUtil.getString(fragmentActivity, R.string.cancel));

        UserInterfaceUtil.showInteractiveInfoDialog(fragmentActivity, title, message, true, true, flPositive, flNegative, new UserInterfaceUtil.DialogAction() {
            @Override
            public void onPositiveAction(Dialog dialog) {
                dialog.dismiss();
                listener.openKhaltiSettings();
            }

            @Override
            public void onNegativeAction(Dialog dialog) {
                dialog.dismiss();
            }
        });
    }

    @Override
    public String getStringFromResource(int id) {
        return ResourceUtil.getString(fragmentActivity, id);
    }

    @Override
    public void openKhaltiSettings() {
        Intent i;
        PackageManager manager = fragmentActivity.getPackageManager();
        try {
            i = manager.getLaunchIntentForPackage("com.khalti.red");
            if (i == null)
                throw new PackageManager.NameNotFoundException();
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(i);
        } catch (PackageManager.NameNotFoundException e) {
            listener.showMessageDialog("Error", ResourceUtil.getString(fragmentActivity, R.string.khalti_not_found));
        }
    }

    @Override
    public void closeWidget() {
        fragmentActivity.finish();
    }

    @Override
    public void toggleConfirmationLayout(boolean show) {
        String buttonText = show ? ResourceUtil.getString(fragmentActivity, R.string.confirm_payment) : "Pay Rs " + NumberUtil.convertToRupees(DataHolder.getConfig().getAmount());
        btnPay.setText(buttonText);
        etCode.setText("");
        etPIN.setText("");
        elConfirmation.toggleExpansion();
    }

    @Override
    public void toggleSmsListener(boolean listen) {
        if (listen) {
            IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
            smsListener = new SmsListener();
            fragmentActivity.registerReceiver(smsListener, intentFilter);
        } else {
            if (EmptyUtil.isNotNull(smsListener)) {
                fragmentActivity.unregisterReceiver(smsListener);
            }
        }
    }

    @Override
    public void setListener(WalletContract.Listener listener) {
        this.listener = listener;
    }
}
