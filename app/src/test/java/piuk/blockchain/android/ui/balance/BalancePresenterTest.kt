package piuk.blockchain.android.ui.balance

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.ethereum.data.EthAddressResponseMap
import io.reactivex.Completable
import io.reactivex.Observable
import org.amshove.kluent.`should equal to`
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.data.access.AuthEvent
import piuk.blockchain.android.data.api.EnvironmentSettings
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.android.data.exchange.BuyDataManager
import piuk.blockchain.android.data.notifications.models.NotificationPayload
import piuk.blockchain.androidcore.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.androidcoreui.ui.base.UiState
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.currency.CryptoCurrencies
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.PrefsUtil

@Suppress("IllegalIdentifier")
class BalancePresenterTest {

    private lateinit var subject: BalancePresenter
    private var view: BalanceView = mock()
    private var exchangeRateFactory: ExchangeRateDataManager = mock()
    private var transactionListDataManager: TransactionListDataManager = mock()
    private var swipeToReceiveHelper: SwipeToReceiveHelper = mock()
    private var payloadDataManager: PayloadDataManager = mock()
    private var buyDataManager: BuyDataManager = mock()
    private var stringUtils: StringUtils = mock()
    private var prefsUtil: PrefsUtil = mock()
    private var currencyState: CurrencyState = mock()
    private var rxBus: RxBus = mock()
    private var ethDataManager: EthDataManager = mock()
    private var shapeShiftDataManager: ShapeShiftDataManager = mock()
    private val bchDataManager: BchDataManager = mock()
    private val walletAccountHelper: WalletAccountHelper = mock()
    private val environmentSettings: EnvironmentSettings = mock()
    private val currencyFormatManager: CurrencyFormatManager = mock()

    @Before
    fun setUp() {

        subject = BalancePresenter(
                exchangeRateFactory,
                transactionListDataManager,
                ethDataManager,
                swipeToReceiveHelper,
                payloadDataManager,
                buyDataManager,
                stringUtils,
                prefsUtil,
                rxBus,
                currencyState,
                shapeShiftDataManager,
                bchDataManager,
                walletAccountHelper,
                environmentSettings,
                currencyFormatManager
        )
        subject.initView(view)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReady() {

        // Arrange
        whenever(environmentSettings.environment).thenReturn(Environment.PRODUCTION)
        val account: ItemAccount = mock()
        whenever(walletAccountHelper.getAccountItemsForOverview()).thenReturn(mutableListOf(account))
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)

        whenever(rxBus.register(NotificationPayload::class.java)).thenReturn(Observable.empty())
        whenever(rxBus.register(AuthEvent::class.java)).thenReturn(Observable.empty())

        // Act
        subject.onViewReady()

        // Assert
        verify(view).setupAccountsAdapter(mutableListOf(account))
        verify(view).setupTxFeedAdapter(true)
    }

    @Test
    @Throws(Exception::class)
    fun onViewDestroyed() {
        // Arrange
        val notificationObservable = Observable.just(NotificationPayload(emptyMap()))
        val authEventObservable = Observable.just(AuthEvent.LOGOUT)
        subject.notificationObservable = notificationObservable
        subject.authEventObservable = authEventObservable
        // Act
        subject.onViewDestroyed()
        // Assert
        verify(rxBus).unregister(NotificationPayload::class.java, notificationObservable)
        verify(rxBus).unregister(AuthEvent::class.java, authEventObservable)
    }

    @Test
    @Throws(Exception::class)
    fun onResume() {
        // Child function onRefreshRequested
    }

    @Test
    @Throws(Exception::class)
    fun areLauncherShortcutsEnabled() {
        // Arrange
        whenever(prefsUtil.getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true))
                .thenReturn(false)
        // Act
        val result = subject.areLauncherShortcutsEnabled()
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true)
        verifyNoMoreInteractions(prefsUtil)
        result `should equal to` false
    }

    @Test
    @Throws(Exception::class)
    fun onRefreshRequested() {
        // Arrange

        //getCurrentAccount()
        whenever(view.getCurrentAccountPosition()).thenReturn(0)
        val account: ItemAccount = mock()
        whenever(walletAccountHelper.getAccountItemsForOverview()).thenReturn(mutableListOf(account))
        whenever(account.displayBalance).thenReturn("0.052 BTC")

        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        whenever(bchDataManager.refreshMetadataCompletable()).thenReturn(Completable.complete())
        whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.empty())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(transactionListDataManager.fetchTransactions(any(), any(), any()))
                .thenReturn(Observable.empty())

        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)

        // Act
        subject.onRefreshRequested()

        // Assert
        verify(view).setUiState(UiState.LOADING)
        verify(view, times(2)).updateSelectedCurrency(CryptoCurrencies.BTC)
        verify(view, times(2)).updateBalanceHeader("0.052 BTC")
        verify(view, times(2)).updateAccountsDataSet(mutableListOf(account))
        verify(view).generateLauncherShortcuts()
        verify(view).updateTransactionValueType(true)

    }

    @Test
    @Throws(Exception::class)
    fun updateBalancesCompletable() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        // Act
        subject.updateBalancesCompletable()
        // Assert
        verify(payloadDataManager).updateAllBalances()

        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.ETHER)
        // Act
        subject.updateBalancesCompletable()
        // Assert
        verify(ethDataManager).fetchEthAddressCompletable()

        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BCH)
        // Act
        subject.updateBalancesCompletable()
        // Assert
        verify(bchDataManager).updateAllBalances()
    }

    @Test
    @Throws(Exception::class)
    fun getUpdateTickerCompletable() {
        // Arrange
        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.getUpdateTickerCompletable().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun updateEthAddress() {
        // Arrange
        val abc: EthAddressResponseMap = mock()
        whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.just(
                CombinedEthModel(
                        abc
                )
        ))
        // Act
        val testObserver = subject.updateEthAddress().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun updateBchWallet() {
        // Arrange
        whenever(bchDataManager.refreshMetadataCompletable()).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.updateBchWallet().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    @Throws(Exception::class)
    fun `onGetBitcoinClicked API less than 19 canBuy returns true`() {
        // Arrange
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(true))
        whenever(view.shouldShowBuy()).thenReturn(false)
        // Act
        subject.onGetBitcoinClicked()
        // Assert
        verify(buyDataManager).canBuy
        verifyNoMoreInteractions(buyDataManager)
        verify(view).shouldShowBuy()
        verify(view).startReceiveFragmentBtc()
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onGetBitcoinClicked API less than 19 canBuy returns false`() {
        // Arrange
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(false))
        whenever(view.shouldShowBuy()).thenReturn(false)
        // Act
        subject.onGetBitcoinClicked()
        // Assert
        verify(buyDataManager).canBuy
        verifyNoMoreInteractions(buyDataManager)
        verify(view).startReceiveFragmentBtc()
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onGetBitcoinClicked canBuy returns true`() {
        // Arrange
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(true))
        whenever(view.shouldShowBuy()).thenReturn(true)
        // Act
        subject.onGetBitcoinClicked()
        // Assert
        verify(buyDataManager).canBuy
        verifyNoMoreInteractions(buyDataManager)
        verify(view).shouldShowBuy()
        verify(view).startBuyActivity()
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onGetBitcoinClicked canBuy returns false`() {
        // Arrange
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(false))
        whenever(view.shouldShowBuy()).thenReturn(true)
        // Act
        subject.onGetBitcoinClicked()
        // Assert
        verify(buyDataManager).canBuy
        verifyNoMoreInteractions(buyDataManager)
        verify(view).startReceiveFragmentBtc()
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun refreshBalanceHeader() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        val account: ItemAccount = mock()
        val value = "0.052 BTC"
        whenever(account.displayBalance).thenReturn(value)
        // Act
        subject.refreshBalanceHeader(account)
        // Assert
        verify(view).updateSelectedCurrency(CryptoCurrencies.BTC)
        verify(view).updateBalanceHeader(value)
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun refreshAccountDataSet() {
        // Arrange
        val mockList = mutableListOf<ItemAccount>()
        whenever(walletAccountHelper.getAccountItemsForOverview()).thenReturn(mockList)
        // Act
        subject.refreshAccountDataSet()
        // Assert
        verify(view).updateAccountsDataSet(mockList)
        verifyNoMoreInteractions(view)
    }
}
