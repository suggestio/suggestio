package util.blocks

import play.api.data._, Forms._
import models._
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 22:02
 * Description: Утиль для блоков, которые содержат в себе списки офферов вместо одного оффера.
 * Почти всегда это блоки title+price.
 */

object ListBlock {

  def mkBfText(bfName: String = Title.BF_NAME_DFLT, offerNopt: Option[Int] = None): BfText = {
    BfText(bfName, maxLen = 128, offerNopt = offerNopt)
  }

  /** Минимальное кол-во полей, отрендеренных сразу. */
  val OFFERS_COUNT_MIN = configuration.getInt("block.offers.count.min") getOrElse 3

  /** Макс. кол-во полей вообще. */
  val OFFERS_COUNT_MAX = configuration.getInt("block.offers.count.max") getOrElse 10

}


import ListBlock._


trait ListBlock extends ValT {
  /** Начало отсчета счетчика офферов. */
  def N0: Int = 0

  /** Макс кол-во офферов (макс.длина списка офферов). */
  def offersCountMax: Int

  protected def offersMapping: Mapping[List[AOBlock]]

  // Mapping
  private def m = offersMapping.withPrefix("offer").withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeOffers = m.bind(data)
    (maybeAcc0, maybeOffers) match {
      case (Right(acc0), Right(offers)) =>
        acc0.offers = offers
        maybeAcc0

      case (Left(accFE), Right(descr)) =>
        maybeAcc0

      case (Right(_), Left(colorFE)) =>
        Left(colorFE)   // Избыточная пересборка left either из-за right-типа. Можно также вернуть через .asInstanceOf, но это плохо.

      case (Left(accFE), Left(colorFE)) =>
        Left(accFE ++ colorFE)
    }
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( value.bd.offers )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.bd.offers
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }

}


/** ListBlock с однотипными полями. */
trait SingleListBlockT extends ListBlock {

  /** Макс кол-во офферов (макс.длина списка офферов). */
  def offersCountMax: Int = OFFERS_COUNT_MAX

  type T1 <: AOValueField
  type BfT1 <: BlockAOValueFieldT { type T = T1 }
  def bf1(offerNopt: Option[Int]): BfT1

  /** Реализация добавления отображаемых полей редактора. */
  def withBlockFieldsRev(af: AdFormM, acc0: List[BlockFieldT], count4render: Int): List[BlockFieldT] = {
    (N0 until count4render).foldLeft(acc0) {
      (acc, offerN) =>
        val offerNopt = Some(offerN)
        val _bf1 = bf1(offerNopt)
        _bf1 :: acc
    }
  }

  /** Генерация описания полей.
    * У нас тут повторяющийся маппинг, поэтому blockFields для редактора генерится без полей-констант. */
  abstract override def blockFieldsRev(af: AdFormM): List[BlockFieldT] = {
    val acc0 = super.blockFieldsRev(af)
    withBlockFieldsRev(af, acc0, offersCountMax)
  }

  protected def offerMapping = {
    mapping(
      bf1(None).getOptionalStrictMappingKV
    )(identity)(Some.apply)
  }

  // Маппинг для списка офферов.
  protected def offersMapping = {
    list(offerMapping)
      //.transform[List[T1]] (_.flatMap(_.iterator), _.map(Some.apply))
      .verifying("error.too.much", { _.size <= offersCountMax })
      .transform[List[AOBlock]] (applyAOBlocks, unapplyAOBlocks)
  }

  /** Собрать AOBlock на основе куска выхлопа формы. */
  protected def applyAOBlocks(l: List[Option[T1]]): List[AOBlock] = {
    l.iterator
      // Делаем zipWithIndex перед фильтром чтобы сохранять выравнивание на странице (css-классы), если 1 или 2 элемент пропущен.
      .zipWithIndex
      // Выкинуть пустые офферы
      .filter {
        case (titleOpt, _) => titleOpt.isDefined
      }
      // Оставшиеся офферы завернуть в AOBlock
      .map {
        case (v1Opt, i) => applyAOBlock(i, v1Opt)
      }
      .toList
  }

  protected def applyAOBlock(offerN: Int, v1: Option[T1]): AOBlock


  /** unapply для offersMapping. Вынесен для упрощения кода. Метод восстанавливает исходный выхлоп формы,
    * даже если были пропущены какие-то группы полей. */
  protected def unapplyAOBlocks(aoBlocks: Seq[AOBlock]): List[Option[T1]] = {
    // без if isEmpty будет экзепшен в maxBy().
    if (aoBlocks.isEmpty) {
      Nil
    } else {
      // Вычисляем оптимальную длину списка результатов
      val maxN = aoBlocks.maxBy(_.n).n
      // Рисуем карту маппингов необходимой длины, ключ - это n.
      val aoBlocksNS = aoBlocks.iterator
        .map { aoBlock => aoBlock.n -> aoBlock }
        .toMap
      // Восстанавливаем новый список выхлопов мапперов на основе длины и имеющихся экземпляров AOBlock.
      (N0 to maxN)
        .map { n =>
          aoBlocksNS
            .get(n)
            .flatMap { unapplyAOBlock }
        }
        .toList
    }
  }

  def unapplyAOBlock(blk: AOBlock): Option[T1]

}


/** List-блок для списка title. */
trait TitleListBlockT extends SingleListBlockT {

  def TITLE_FN = Title.BF_NAME_DFLT
  override type T1 = AOStringField
  override type BfT1 = BfText
  override def bf1(offerNopt: Option[Int]) = ListBlock.mkBfText(TITLE_FN, offerNopt)
  def titleBf = bf1(None)

  override protected def applyAOBlock(offerN: Int, v1: Option[T1]): AOBlock = {
    AOBlock(n = offerN, text1 = v1)
  }

  override def unapplyAOBlock(blk: AOBlock): Option[T1] = {
    blk.text1
  }

  /** Реализация добавления отображаемых полей редактора с учётом реально необходимого кол-ва полей. */
  override def withBlockFieldsRev(af: AdFormM, acc0: List[BlockFieldT], count4render0: Int): List[BlockFieldT] = {
    // Нужно определить, сколько именно нужно рендерить полей.
    import controllers.ad.MarketAdFormUtil.{AD_K, OFFER_K}
    val allOfferNs = af(s"$AD_K.$OFFER_K.$OFFER_K").indexes
    val offersCount = if (allOfferNs.nonEmpty) {
      Math.max(OFFERS_COUNT_MIN, allOfferNs.max + 1)
    } else {
      OFFERS_COUNT_MIN
    }
    val acc1 = super.withBlockFieldsRev(af, acc0, offersCount)
    // Добавить поле добавления нового текстового поля, если есть куда.
    if (offersCount <= OFFERS_COUNT_MAX) {
      BfAddStringField() :: acc1
    } else {
      acc1
    }
  }
}
