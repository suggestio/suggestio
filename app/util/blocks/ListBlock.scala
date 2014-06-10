package util.blocks

import play.api.data._, Forms._
import models._
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 22:02
 * Description: Утиль для блоков, которые содержат в себе списки офферов вместо одного оффера.
 * Почти всегда это блоки title+price.
 */

object ListBlock {

  def mkBfText(bfName: String, offerNopt: Option[Int]): BfText = {
    BfText(bfName, BlocksEditorFields.TextArea, maxLen = 128, offerNopt = offerNopt)
  }

}


/** Абстрактная платформа блока для списка пар. Работает с AOValueField, но можно снять это ограничение в будущем. */
trait PairListBlock extends ValT {
  type T1 <: AOValueField
  type BfT1 <: BlockAOValueFieldT { type T = T1 }
  def bf1(offerNopt: Option[Int]): BfT1

  type T2 <: AOValueField
  type BfT2 <: BlockAOValueFieldT { type T = T2 }
  def bf2(offerNopt: Option[Int]): BfT2

  /** Начало отсчета счетчика офферов. */
  def N0 = 0

  /** Макс кол-во офферов (макс.длина списка офферов). */
  def offersCount: Int


  /** Генерация описания полей. У нас тут повторяющийся маппинг, поэтому blockFields для редактора генерится без полей-констант. */
  abstract override def blockFieldsRev: List[BlockFieldT] = {
    val acc0 = super.blockFieldsRev
    (N0 until offersCount).foldLeft(acc0) {
      (acc, offerN) =>
        val offerNopt = Some(offerN)
        val _bf1 = bf1(offerNopt)
        val _bf2 = bf2(offerNopt)
        _bf2 :: _bf1 :: acc
    }
  }



  // Маппинг для одного элемента (оффера)
  protected def offerMapping = tuple(
    bf1(None).getOptionalStrictMappingKV,
    bf2(None).getOptionalStrictMappingKV
  )
  // Маппинг для списка офферов.
  protected def offersMapping = list(offerMapping)
    .verifying("error.too.much", { _.size <= offersCount })
    .transform[List[AOBlock]] (applyAOBlocks, unapplyAOBlocks)


  /** Собрать AOBlock на основе куска выхлопа формы. */
  protected def applyAOBlocks(l: List[(Option[T1], Option[T2])]): List[AOBlock] = {
    l.iterator
      // Делаем zipWithIndex перед фильтром чтобы сохранять выравнивание на странице (css-классы), если 1 или 2 элемент пропущен.
      .zipWithIndex
      // Выкинуть пустые офферы
      .filter {
      case ((titleOpt, priceOpt), _) =>
        titleOpt.isDefined || priceOpt.isDefined
    }
      // Оставшиеся офферы завернуть в AOBlock
      .map {
      case ((v1Opt, v2Opt), i) =>
        applyAOBlock(i, v1Opt, v2Opt)
    }
      .toList
  }

  protected def applyAOBlock(offerN: Int, v1: Option[T1], v2: Option[T2]): AOBlock


  /** unapply для offersMapping. Вынесен для упрощения кода. Метод восстанавливает исходный выхлоп формы,
    * даже если были пропущены какие-то группы полей. */
  protected def unapplyAOBlocks(aoBlocks: Seq[AOBlock]): List[(Option[T1], Option[T2])] = {
    // без if isEmpty будет экзепшен в maxBy().
    if (aoBlocks.isEmpty) {
      Nil
    } else {
      // Вычисляем оптимальную длину списка результатов
      val maxN = aoBlocks.maxBy(_.n).n
      // Рисуем карту маппингов необходимой длины, ключ - это n.
      val aoBlocksNS = aoBlocks
        .map { aoBlock => aoBlock.n -> aoBlock }
        .toMap
      // Восстанавливаем новый список выхлопов мапперов на основе длины и имеющихся экземпляров AOBlock.
      (N0 to maxN)
        .map { n =>
          aoBlocksNS
            .get(n)
            .map { unapplyAOBlock }
            .getOrElse(None -> None)
        }
        .toList
    }
  }

  def unapplyAOBlock(blk: AOBlock): (Option[T1], Option[T2])

  // Mapping
  private def m = offersMapping.withPrefix("offer").withPrefix(key)


  abstract override def mappingsAcc: List[Mapping[_]] = {
    val m1 = m
    m1 :: super.mappingsAcc
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


/** Для сборки блоков, обрабатывающие блоки с офферами вида "title+price много раз", используется этот трейт. */
trait TitlePriceListBlockT extends PairListBlock {

  def TITLE_FN = Title.BF_NAME_DFLT
  def PRICE_FN = Price.BF_NAME_DFLT

  override type T1 = AOStringField
  override type BfT1 = BfText
  override def bf1(offerNopt: Option[Int]) = ListBlock.mkBfText(TITLE_FN, offerNopt)

  override type T2 = AOPriceField
  override type BfT2 = BfPrice
  override def bf2(offerNopt: Option[Int]) = {
    BfPrice(PRICE_FN, offerNopt = offerNopt)
  }


  def titleBf = bf1(None)
  def priceBf = bf2(None)

  override def applyAOBlock(offerN: Int, v1: Option[T1], v2: Option[T2]): AOBlock = {
    AOBlock(n = offerN, text1 = v1, price = v2)
  }


  override def unapplyAOBlock(blk: AOBlock): (Option[T1], Option[T2]) = {
    blk.text1 -> blk.price
  }

}


/** Блок для списка title-descr. В целом аналогичен TitlePriceListBlockT. */
trait TitleDescrListBlockT extends PairListBlock {
  // TODO 2014.06.10 titleBf(), descrBf() методы сделаны по аналогии, но не используются пока что. Можно их удалить, если не понадобятся.

  def TITLE_FN = Title.BF_NAME_DFLT
  override type T1 = AOStringField
  override type BfT1 = BfText
  override def bf1(offerNopt: Option[Int]) = ListBlock.mkBfText(TITLE_FN, offerNopt)
  def titleBf = bf1(None)

  def DESCR_FN = Descr.BF_NAME_DFLT
  override type T2 = AOStringField
  override type BfT2 = BfText
  override def bf2(offerNopt: Option[Int]) = ListBlock.mkBfText(DESCR_FN, offerNopt)
  def descrBf = bf2(None)

  override def applyAOBlock(offerN: Int, v1: Option[T1], v2: Option[T2]): AOBlock = {
    AOBlock(n = offerN, text1 = v1, text2 = v2)
  }

  override def unapplyAOBlock(blk: AOBlock): (Option[T1], Option[T2]) = {
    blk.text1 -> blk.text2
  }
}

