package io.suggest.sc.sjs.m.mfoc

import io.suggest.common.m.mad.IMadId
import io.suggest.sjs.common.model.MHand

object MFocSd {

  /** Добавление новых элементов карусели слева, сохраняя исходный порядок. */
  def appendFadsLeft(fads0: FAdQueue, newFads: TraversableOnce[IFocAd]): FAdQueue = {
    newFads.foldRight(fads0) { (fad, fadsQ) =>
      fad +: fadsQ
    }
  }

}


/** Интерфейс контейнера данных по focused-выдаче. */
trait IFocSd {

  /** Состояние отображения текущей карточки. */
  def current: MFocCurrSd

  /** id продьюсера, в рамках которого идёт просмотр карточек. */
  def producerId: Option[String]

  /** Общее кол-во карточек со всех возможных выборов в рамках задачи.
    * Если None, значит точное кол-во пока не известно. */
  def totalCount: Option[Int]

  /** Список карточек, уже полученных с сервера. */
  def fads: Seq[IFocAd]

  /** Неупорядоченная карта известных focused-карточек, полученных с сервера.
    * По идее, она реализуется через lazy val поверх fadsQueue. */
  def fadsMap: Map[String, IFocAd]

  /** Текущее выставленное направление стрелки, которая рядом с курсором мыши бегает по экрану.
    * Название поля -- сокращение от "arrow direction". */
  def arrDir: Option[MHand]

  /** Опциональные данные состояния touch-навигации внутри focused-выдачи. */
  def touch: Option[MFocTouchSd]

  // 2015.jun.3 Отказ от индексов вынуждает выявлять id крайних карточек на лету, анализируя возвращаемые сервером сегменты цепочки карточек.
  /** Точный id первой карточки, выявленный в ходе запросов сегментов foc-выдачи. */
  def firstAdId  : Option[String]

  /** Точно id последней карточки, выявленный в ходе запросов сегментов foc-выдачи. */
  def lastAdId   : Option[String]

  /** Инфа о текущем реквесте к серверу, если он имеет место быть. */
  def req         : Option[MFocReqInfo]


  def fadsAfterCurrentIter  = IMadId.adsAfter(current.madId, fads)
  def fadsBeforeCurrentIter = IMadId.adsBefore(current.madId, fads)

  def fadIdsIter = fads.iterator.map(_.madId)

  /** Является ли текущая ad_id крайней справа?
    * @return true -- достоверно последняя,
    *         false -- скорее всего карточка не последняя.
    */
  def isCurrAdFirst = firstAdId.contains(current.madId)
  def isCurrAdLast  = lastAdId.contains(current.madId)

  // Короткий код для поиска текущей карточки в кеше всех карточек.
  def findCurrFad = fadsMap.get( current.madId )

}


/**
 * Реализация контейнера данных состояния FSM Focused-выдачи.
 * Контейнер собирается ещё до открытия focused-выдачи для передачи начальных данных.
 */
case class MFocSd(
  override val current      : MFocCurrSd,
  override val producerId   : Option[String],
  override val totalCount   : Option[Int]           = None,
  override val fads         : FAdQueue              = Nil,
  override val arrDir       : Option[MHand]         = None,
  override val touch        : Option[MFocTouchSd]   = None,
  override val firstAdId    : Option[String]        = None,
  override val lastAdId     : Option[String]        = None,
  override val req          : Option[MFocReqInfo]   = None
)
  extends IFocSd
{

  override lazy val fadsMap: Map[String, IFocAd] = {
    fads.iterator
      .map { fad => fad.madId -> fad }
      .toMap
  }

}
