package khalti.checkOut.EBanking;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.jakewharton.rxbinding.view.RxView;

import java.util.HashMap;
import java.util.List;

import khalti.R;
import khalti.carbonX.widget.Button;
import khalti.carbonX.widget.FrameLayout;
import khalti.carbonX.widget.ProgressBar;
import khalti.checkOut.EBanking.contactForm.ContactFormFragment;
import khalti.checkOut.EBanking.helper.BankAdapter;
import khalti.checkOut.EBanking.helper.BankPojo;
import khalti.checkOut.EBanking.helper.EBankingData;
import khalti.utils.NetworkUtil;
import khalti.utils.ResourceUtil;
import rx.Observable;

public class EBanking extends Fragment implements EBankingContract.View {

    private RecyclerView rvList;
    private LinearLayout llIndented;
    private ProgressBar pdLoad;
    private AppCompatTextView tvMessage;
    private FrameLayout flTryAgain;
    private Button btnTryAgain;
    private AppBarLayout appBarLayout;

    private FragmentActivity fragmentActivity;
    private EBankingContract.Presenter presenter;
    private BankAdapter bankAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.ebanking, container, false);
        fragmentActivity = getActivity();
        presenter = new EBankingPresenter(this);

        rvList = mainView.findViewById(R.id.rvList);
        llIndented = mainView.findViewById(R.id.llIndented);
        pdLoad = mainView.findViewById(R.id.pdLoad);
        tvMessage = mainView.findViewById(R.id.tvMessage);
        flTryAgain = mainView.findViewById(R.id.flTryAgain);
        btnTryAgain = mainView.findViewById(R.id.btnTryAgain);
        appBarLayout = mainView.findViewById(R.id.appBar);

        presenter.onCreate(NetworkUtil.isNetworkAvailable(fragmentActivity));

        return mainView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
    }

    @Override
    public void toggleIndented(boolean show) {
        llIndented.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setUpList(List<BankPojo> bankList) {
        appBarLayout.setVisibility(View.VISIBLE);
        rvList.setVisibility(View.VISIBLE);
        bankAdapter = new BankAdapter(fragmentActivity, bankList);
        rvList.setAdapter(bankAdapter);
        rvList.setHasFixedSize(false);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(fragmentActivity, 3);
        rvList.setLayoutManager(layoutManager);
    }

    @Override
    public void showIndentedNetworkError() {
        pdLoad.setVisibility(View.INVISIBLE);
        flTryAgain.setVisibility(View.INVISIBLE);
        tvMessage.setVisibility(View.VISIBLE);
        tvMessage.setText(ResourceUtil.getString(fragmentActivity, R.string.network_error_body));
    }

    @Override
    public void showIndentedError(String error) {
        pdLoad.setVisibility(View.INVISIBLE);
        flTryAgain.setVisibility(View.VISIBLE);
        tvMessage.setVisibility(View.VISIBLE);
        tvMessage.setText(error);
    }

    @Override
    public void openMobileForm(EBankingData eBankingData) {
        ContactFormFragment contactFormFragment = new ContactFormFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("data", eBankingData);
        contactFormFragment.setArguments(bundle);
        contactFormFragment.show(getFragmentManager(), contactFormFragment.getTag());
    }

    @Override
    public Observable<HashMap<String, String>> getItemClickObservable() {
        return bankAdapter.getItemClickObservable();
    }

    @Override
    public Observable<Void> setTryAgainClick() {
        return RxView.clicks(btnTryAgain);
    }

    @Override
    public void setPresenter(EBankingContract.Presenter presenter) {
        this.presenter = presenter;
    }
}