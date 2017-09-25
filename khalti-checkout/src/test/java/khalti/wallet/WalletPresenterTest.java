package khalti.wallet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import khalti.checkOut.Wallet.WalletConfirmPojo;
import khalti.checkOut.Wallet.WalletContract;
import khalti.checkOut.Wallet.WalletModel;
import khalti.checkOut.Wallet.WalletPresenter;
import khalti.checkOut.api.Config;
import khalti.checkOut.api.ErrorAction;
import khalti.checkOut.api.OnCheckOutListener;
import khalti.utils.Store;
import rx.Subscription;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
@PrepareForTest({WalletPresenter.class, Store.class})
public class WalletPresenterTest {
    private WalletPresenter walletPresenter;

    @Mock
    private WalletContract.View mWalletView;
    @Mock
    private WalletModel walletModel;
    @Mock
    private Config config;
    @Mock
    private OnCheckOutListener onCheckOutListener;
    @Captor
    private ArgumentCaptor<WalletModel.WalletAction> walletArgument;
    @Mock
    private Subscription subscription;
    @Mock
    private WalletConfirmPojo walletConfirmPojo;

    private String mobile = "9800000000";
    private String confirmationCode = "123";
    private String pin = "0000";

    @Before
    public void setWalletPresenter() throws Exception {
        MockitoAnnotations.initMocks(this);

        PowerMockito.whenNew(WalletModel.class).withNoArguments().thenReturn(walletModel);
        PowerMockito.mockStatic(Store.class);

        walletPresenter = new WalletPresenter(mWalletView);
        config = new Config("public_key", "product_id", "product_name", "product_url", 1L, onCheckOutListener);

        PowerMockito.when(Store.getConfig()).thenReturn(config);
        Mockito.when(walletModel.initiatePayment(eq(mobile), eq(config), walletArgument.capture())).thenReturn(subscription);
        Mockito.when(walletModel.confirmPayment(eq(confirmationCode), eq(pin), walletArgument.capture())).thenReturn(subscription);

        walletPresenter.injectModel(walletModel);
        walletPresenter.injectConfig(config);
    }

    @Test
    public void setUpLayout() {
        walletPresenter.setUpLayout();
        verify(mWalletView).setButtonText(Mockito.anyString());
        verify(mWalletView).setButtonClickListener();
    }

    @Test
    public void initiatePaymentWithoutNetwork() {
        walletPresenter.initiatePayment(false, mobile);
        verify(mWalletView).showNetworkError();
    }

    @Test
    public void initiatePaymentWithEmptyMobile() {
        walletPresenter.initiatePayment(true, "");
        verify(mWalletView).setEditTextError("mobile", "This field is required");
    }

    @Test
    public void initiatePaymentWithInvalidMobile() {
        walletPresenter.initiatePayment(true, "8000000000");
        verify(mWalletView).setEditTextError("mobile", "Invalid mobile number");
    }

    @Test
    public void successfulPaymentInitiation() {
        walletPresenter.initiatePayment(true, mobile);
        verify(mWalletView).toggleProgressDialog("init", true);
        verify(walletModel).initiatePayment(eq(mobile), eq(config), walletArgument.capture());
        walletArgument.getValue().onCompleted(null);
        verify(mWalletView).setEditTextListener();
        verify(mWalletView).toggleSmsListener(true);
        verify(mWalletView).toggleProgressDialog("init", false);
        verify(mWalletView).toggleConfirmationLayout(true);
    }

    @Test
    public void failedPaymentInitiationWithNoPIN() {
        walletPresenter.initiatePayment(true, mobile);
        verify(mWalletView).toggleProgressDialog("init", true);
        verify(walletModel).initiatePayment(eq(mobile), eq(config), walletArgument.capture());
        walletArgument.getValue().onError("</a>");
        verify(mWalletView).showPINDialog(anyString(), anyString());
        verify(config.getOnCheckOutListener()).onError(anyString(), anyString());
    }

    @Test
    public void failedPaymentInitiation() {
        walletPresenter.initiatePayment(true, mobile);
        verify(mWalletView).toggleProgressDialog("init", true);
        verify(walletModel).initiatePayment(eq(mobile), eq(config), walletArgument.capture());
        walletArgument.getValue().onError("");
        verify(mWalletView).showMessageDialog(Mockito.anyString(), Mockito.anyString());
        verify(config.getOnCheckOutListener()).onError(eq(ErrorAction.WALLET_INITIATE.getAction()), anyString());
    }

    @Test
    public void confirmPaymentWithoutNetwork() {
        walletPresenter.confirmPayment(false, confirmationCode, pin);
        verify(mWalletView).showNetworkError();
    }

    @Test
    public void confirmPaymentWithBothParamEmpty() {
        walletPresenter.confirmPayment(true, "", "");
        verify(mWalletView).setEditTextError(eq("code"), eq("This field is required"));
        verify(mWalletView).setEditTextError(eq("pin"), eq("This field is required"));
    }

    @Test
    public void confirmPaymentWithEmptyCode() {
        walletPresenter.confirmPayment(true, "", pin);
        verify(mWalletView).setEditTextError(eq("code"), eq("This field is required"));
    }

    @Test
    public void confirmPaymentWithEmptyPIN() {
        walletPresenter.confirmPayment(true, confirmationCode, "");
        verify(mWalletView).setEditTextError(eq("pin"), eq("This field is required"));
    }

    @Test
    public void failedPaymentConfirmation() {
        walletPresenter.confirmPayment(true, confirmationCode, pin);
        verify(mWalletView).toggleProgressDialog("confirm", true);
        verify(walletModel).confirmPayment(eq(confirmationCode), eq(pin), walletArgument.capture());
        walletArgument.getValue().onError(eq(anyString()));
        verify(mWalletView).toggleProgressDialog("confirm", false);
        verify(mWalletView).showMessageDialog(eq("Error"), anyString());
        verify(config.getOnCheckOutListener()).onError(eq(ErrorAction.WALLET_CONFIRM.getAction()), anyString());
    }

    @Test
    public void successfulPaymentConfirmation() {
        walletPresenter.confirmPayment(true, confirmationCode, pin);
        verify(mWalletView).toggleProgressDialog("confirm", true);
        verify(walletModel).confirmPayment(eq(confirmationCode), eq(pin), walletArgument.capture());
        walletArgument.getValue().onCompleted(walletConfirmPojo);
        verify(mWalletView).toggleProgressDialog("confirm", false);
        verify(onCheckOutListener).onSuccess(any());
        verify(mWalletView).closeWidget();
    }
}
