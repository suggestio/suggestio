package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.ScConstants.Focused.SIDE_PRELOAD_MAX
import io.suggest.sc.sjs.c.scfsm.FindAdsUtil
import io.suggest.sc.sjs.m.mfoc.{FAdQueue, MFocSd}
import io.suggest.sc.sjs.m.msrv.foc.find.{MFocAdSearchNoOpenIndex, MFocAd, MFocAds, MFocAdSearchEmpty}
import io.suggest.sc.sjs.vm.grid.GBlock
import io.suggest.sc.sjs.vm.res.FocusedRes
import io.suggest.sjs.common.model.MHand
import io.suggest.sjs.common.msg.ErrorMsgs

import scala.annotation.tailrec
import scala.util.Failure
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.08.15 10:19
 * Description: Аддон для [[io.suggest.sc.sjs.c.scfsm.ScFsm]], добавляющий поддержку сборки состояний,
 * связанных с нахождением в focused-выдаче и фоновой подгрузкой карточек.
 *
 * Считаем, что состояния вызываются только когда 100% нужно получить правые/левые карточки с сервера.
 * Так же считается, что текущая карточка -- крайняя, дальше уже незагруженные карточки.
 * Если юзер запросил листание на карточку во время её preload'а, то надо перекинуть его на Loader.
 */
trait PreLoading extends OnFocusBase {


  /** Абстрактная логика инициализации состояний обычного preload-а без touch. */
  protected trait FocPreLoadingStateT extends OnFocusStateBaseT with FocPreLoadingReceiveStateT with FindAdsUtil {

    override def afterBecome(): Unit = {
      super.afterBecome()
      val sd0 = _stateData
      for {
        fState      <- sd0.focused
        currIndex   <- fState.currIndex
        currFAd     <- fState.shownFadWithIndex(currIndex)
      } {
        // Какое кол-во карточек надо запросить с сервера?
        val resultsLimit = _getLimit(fState, currIndex)
        assert(resultsLimit >= 0)

        // Сборка значения firstAdIds.
        val _firstAdIds: List[String] = if (fState.currAdLookup) {
          // Ручное управление фокусировкой. Плитка может отсутствовать или быть неактуальной.
          Nil

        } else {
          // Нужно попытаться собрать id недостающих карточек из текущей сетки. Данные по текущим карточкам живут прямо в самой плитке.
          // Но плитки или необходимых данных в ней может и не быть (карточка отсутствует), это надо учитывать.
          val maybeFirstAdIds = for (currBlock <- GBlock.find(currFAd.madId)) yield {
            @tailrec def __collectBlocks(needCount: Int, lastBlock: GBlock, acc0: List[String]): (Int, List[String]) = {
              val maybeNextBlockInfo = if (needCount > 0) {
                for {
                  nextBlock <- _nextGBlock( lastBlock )
                  madId     <- nextBlock.madId
                } yield {
                  madId -> nextBlock
                }
              } else {
                None
              }
              maybeNextBlockInfo match {
                case Some((madId, nextBlock)) =>
                  __collectBlocks(needCount - 1, nextBlock, madId :: acc0)
                case None =>
                  (needCount, acc0)
              }
            }
            __collectBlocks(resultsLimit, currBlock, Nil)
          }

          maybeFirstAdIds.fold( List.empty[String] ) { res =>
            _fixAdIdsAccOrder(res._2)
          }
        }

        val _offset = _getOffset(currIndex, resultsLimit)

        val reqArgs = new MFocAdSearchEmpty with FindAdsArgsT with MFocAdSearchNoOpenIndex {
          override def _sd        = sd0
          override def firstAdIds = _firstAdIds
          // Выставляем под нужды focused-выдачи значения limit/offset.
          override def offset     = Some(_offset)
          override def limit      = Some(resultsLimit)
        }
        val fadsFut = MFocAds.find(reqArgs)

        // Когда придёт результат запроса, надо уведомить FSM.
        _sendFutResBack(fadsFut)
      }   // for fState...
    }     // afterBecome


    protected def _fixAdIdsAccOrder(acc: List[String]): List[String]
    protected def _nextGBlock(gblock: GBlock): Option[GBlock]
    protected def _getLimit(fState: MFocSd, currIndex: Int): Int
    protected def _getOffset(currIndex: Int, limit: Int): Int

  }
  
  
  /** Логика получения и обработки ответа прелоада. */
  protected trait FocPreLoadingReceiveStateT extends FsmEmptyReceiverState {

    private def _receiverPart: Receive = {
      case mfa: MFocAds =>
        _adsReceived(mfa)
      case Failure(ex) =>
        _adsRequestFailed(ex)
    }

    override def receiverPart: Receive = {
      _receiverPart orElse super.receiverPart
    }

    /** Реакция на получение ответа сервера на поисковый запрос. */
    protected def _adsReceived(mfa: MFocAds): Unit = {
      // Слили сразу заливаем в DOM. Может это конечно несовсем правильно, но в целом норм.
      for (styles <- mfa.styles; res <- FocusedRes.find()) {
        res.appendCss(styles)
      }
      // Закинуть в соответствующий аккамулятор состояния новые элементы.
      val sd0 = _stateData
      val sd1 = sd0.copy(
        focused = sd0.focused
          .map { fState =>
            val q2 = mfa.focusedAdsIter
              .foldLeft( _getQueue(fState) ) { _enqueueFad }
            _setAdsReceived(q2, fState)
          }
      )
      become(_preloadDoneState, sd1)
    }

    /** Реакция на ошибку в поисковом запросе focused-карточек. */
    protected def _adsRequestFailed(ex: Throwable): Unit = {
      error(ErrorMsgs.FOC_PRELOAD_REQUEST_FAILED, ex)
      // Надо вернутся на предыдущее состояние.
      become(_preloadDoneState)
    }

    protected def _preloadDoneState: FsmState

    protected def _enqueueFad(q: FAdQueue, fad: MFocAd): FAdQueue
    protected def _getQueue(fState: MFocSd): FAdQueue
    protected def _setAdsReceived(q2: FAdQueue, fState: MFocSd): MFocSd

  }


  /** Реализация ресевера результат запроса FocPreLoadingReceiveStateT для подгрузки справа. */
  protected trait ForRightPreLoadingReceiveStateT extends FocPreLoadingReceiveStateT {

    override protected def _getQueue(fState: MFocSd): FAdQueue = {
      fState.nexts
    }

    override protected def _enqueueFad(q: FAdQueue, fad: MFocAd): FAdQueue = {
      q.enqueue(fad)
    }

    override protected def _setAdsReceived(q2: FAdQueue, fState: MFocSd): MFocSd = {
      fState.copy(
        nexts = q2
      )
    }

  }

  /** Трейт для сборки состояний опережающей подгрузки focused-карточек
    * во время нахождения юзера в выдаче с обработкой всех сигналов UI. */
  protected trait FocRightPreLoadingStateT extends FocPreLoadingStateT with ForRightPreLoadingReceiveStateT {

    override protected def _filterSimpleShiftSignal(mhand: MHand): Boolean = {
      !mhand.isRight
    }

    override protected def _fixAdIdsAccOrder(acc: List[String]): List[String] = {
      acc.reverse
    }

    override protected def _nextGBlock(gblock: GBlock): Option[GBlock] = {
      gblock.next
    }


    override protected def _getOffset(currIndex: Int, limit: Int): Int = {
      currIndex + 1
    }

    override protected def _getLimit(fState: MFocSd, currIndex: Int): Int = {
      val spm = SIDE_PRELOAD_MAX
      fState.totalCount.fold(spm) {totalCount =>
        val sideMax = totalCount - currIndex - 1
        Math.min(spm, sideMax)
      }
    }

  }


  /** Реализация обработки полученного от сервера ответа с preload-карточками. */
  protected trait ForLeftPreLoadingReceiveStateT extends FocPreLoadingReceiveStateT {

    override protected def _getQueue(fState: MFocSd): FAdQueue = {
      fState.prevs
    }

    override protected def _enqueueFad(q: FAdQueue, fad: MFocAd): FAdQueue = {
      fad +: q
    }

    override protected def _setAdsReceived(q2: FAdQueue, fState: MFocSd): MFocSd = {
      fState.copy(
        prevs = q2
      )
    }

  }


  /** Трейт для реализации состояния предзагрузки карточек слева от текущей. */
  protected trait FocLeftPreLoadingStateT extends FocPreLoadingStateT with ForLeftPreLoadingReceiveStateT {

    override protected def _filterSimpleShiftSignal(mhand: MHand): Boolean = {
      !mhand.isLeft
    }

    override protected def _fixAdIdsAccOrder(acc: List[String]): List[String] = {
      acc
    }

    override protected def _nextGBlock(gblock: GBlock): Option[GBlock] = {
      gblock.previous
    }

    override protected def _getOffset(currIndex: Int, limit: Int): Int = {
      currIndex - limit
    }

    override protected def _getLimit(fState: MFocSd, currIndex: Int): Int = {
      val sideMax = currIndex
      Math.min(SIDE_PRELOAD_MAX, sideMax)
    }

  }

}
