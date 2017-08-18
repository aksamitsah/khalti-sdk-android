package com.khalti.form.Wallet;

import android.support.annotation.NonNull;

import com.khalti.R;
import com.khalti.utils.DataHolder;
import com.khalti.utils.Event;
import com.khalti.widget.basic.Pay;
import com.utila.EmptyUtil;
import com.utila.ErrorUtil;
import com.utila.GuavaUtil;
import com.utila.NumberUtil;
import com.utila.StringUtil;
import com.utila.ValidationUtil;

import rx.subscriptions.CompositeSubscription;

class WalletPresenter implements WalletContract.Listener {
    @NonNull
    private final WalletContract.View mWalletView;
    private WalletModel walletModel;
    private CompositeSubscription compositeSubscription;
    private boolean smsListenerInitialized = false;

    WalletPresenter(@NonNull WalletContract.View mWalletView) {
        this.mWalletView = GuavaUtil.checkNotNull(mWalletView);
        mWalletView.setListener(this);
        walletModel = new WalletModel();
    }

    @Override
    public void setUpLayout() {
        mWalletView.setEditTextListener();
        mWalletView.setButtonText("Pay Rs " + StringUtil.formatNumber(NumberUtil.convertToRupees(DataHolder.getConfig().getAmount())));
        mWalletView.setButtonClickListener();
    }

    @Override
    public void setConfirmationCode(Event event) {
        mWalletView.setConfirmationCode(event.getEvent().toString().replaceAll("\\D+", ""));
    }

    @Override
    public void toggleConfirmationLayout(boolean show) {
        mWalletView.toggleConfirmationLayout(show);
    }

    @Override
    public void toggleSmsListener(boolean listen) {
        mWalletView.toggleSmsListener(listen);
        smsListenerInitialized = false;
    }

    @Override
    public void openKhaltiSettings() {
        mWalletView.openKhaltiSettings();
    }

    @Override
    public void showMessageDialog(String title, String message) {
        mWalletView.showMessageDialog(title, message);
    }

    @Override
    public void initiatePayment(boolean isNetwork, String mobile) {
        if (isNetwork) {
            if (EmptyUtil.isNotEmpty(mobile) && ValidationUtil.isMobileNumberValid(mobile)) {
                mWalletView.toggleProgressDialog("init", true);
                compositeSubscription = new CompositeSubscription();
                compositeSubscription.add(walletModel.initiatePayment(mobile, DataHolder.getConfig(), new WalletModel.WalletAction() {
                    @Override
                    public void onCompleted() {
                        mWalletView.toggleSmsListener(!smsListenerInitialized);
                        smsListenerInitialized = true;
                        mWalletView.toggleProgressDialog("init", false);
                        mWalletView.toggleConfirmationLayout(true);
                    }

                    @Override
                    public void onError(String message) {
                        mWalletView.toggleProgressDialog("init", false);
                        String error = ErrorUtil.parseError(message);
                        if (error.contains("</a>")) {
                            mWalletView.showInteractiveMessageDialog("Error", mWalletView.getStringFromResource(R.string.pin_not_set));
                        } else {
                            mWalletView.showMessageDialog("Error", error);
                        }
                    }
                }));
            } else {
                if (EmptyUtil.isEmpty(mobile)) {
                    mWalletView.setEditTextError("mobile", "This field is required");
                } else {
                    mWalletView.setEditTextError("mobile", "Invalid mobile number");
                }
            }
        } else {
            mWalletView.showNetworkError();
        }
    }

    @Override
    public void confirmPayment(boolean isNetwork, String confirmationCode, String transactionPin) {
        if (isNetwork) {
            if (EmptyUtil.isNotEmpty(confirmationCode) && EmptyUtil.isNotEmpty(transactionPin)) {
                mWalletView.toggleProgressDialog("confirm", true);
                compositeSubscription = new CompositeSubscription();

                compositeSubscription.add(walletModel.confirmPayment(confirmationCode, transactionPin, new WalletModel.WalletAction() {
                    @Override
                    public void onCompleted() {
                        mWalletView.toggleProgressDialog("confirm", false);
                        Pay.OnSuccessListener onSuccessListener = DataHolder.getOnSuccessListener();
                        onSuccessListener.onSuccess();
                        mWalletView.closeWidget();
                    }

                    @Override
                    public void onError(String message) {
                        mWalletView.toggleProgressDialog("confirm", false);
                        mWalletView.showMessageDialog("Error", ErrorUtil.parseError(message));
                    }
                }));
            } else {
                if (EmptyUtil.isEmpty(confirmationCode) && EmptyUtil.isEmpty(transactionPin)) {
                    mWalletView.setEditTextError("code", "This field is required");
                    mWalletView.setEditTextError("pin", "This field is required");
                } else if (EmptyUtil.isEmpty(confirmationCode)) {
                    mWalletView.setEditTextError("code", "This field is required");
                } else {
                    mWalletView.setEditTextError("pin", "This field is required");
                }
            }
        } else {
            mWalletView.showNetworkError();
        }
    }

    @Override
    public void unSubscribe() {
        if (compositeSubscription.hasSubscriptions() && !compositeSubscription.isUnsubscribed()) {
            compositeSubscription.unsubscribe();
        }
    }
}
