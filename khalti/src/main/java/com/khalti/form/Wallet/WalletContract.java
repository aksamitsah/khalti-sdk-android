package com.khalti.form.Wallet;

import com.khalti.utils.Event;

interface WalletContract {
    interface View {

        void toggleProgressDialog(String action, boolean show);

        void toggleConfirmationLayout(boolean show);

        void toggleSmsListener(boolean listen);

        void setEditTextListener();

        void setEditTextError(String view, String error);

        void setButtonText(String text);

        void setButtonClickListener();

        void setConfirmationCode(String code);

        void showNetworkError();

        void showMessageDialog(String title, String message);

        void showInteractiveMessageDialog(String title, String message);

        void openKhaltiSettings();

        void closeWidget();

        String getStringFromResource(int id);

        void setListener(Listener listener);
    }

    interface Listener {

        void setUpLayout();

        void setConfirmationCode(Event event);

        void toggleConfirmationLayout(boolean show);

        void toggleSmsListener(boolean listen);

        void openKhaltiSettings();

        void showMessageDialog(String title, String message);

        void initiatePayment(boolean isNetwork, String mobile);

        void confirmPayment(boolean isNetwork, String confirmationCode, String transactionPin);

        void unSubscribe();
    }
}
