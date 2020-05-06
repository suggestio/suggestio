package util.ad

import javax.inject.{Inject, Singleton}
import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths}
import io.suggest.color.MColorData
import io.suggest.common.empty.OptionUtil
import io.suggest.err.ErrorConstants
import io.suggest.jd._
import io.suggest.jd.tags._
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.n2.edge.{EdgeUid_t, MPredicates}
import io.suggest.n2.media.MFileMeta
import io.suggest.pick.ContentTypeCheck
import io.suggest.scalaz.{ScalazUtil, StringValidationNel}
import io.suggest.text.StringUtil.StringCollUtil
import io.suggest.up.UploadConstants
import io.suggest.util.logs.MacroLogsImpl
import japgolly.univeq._
import models.mctx.Context
import play.api.{Environment, Mode}
import io.suggest.primo.id._
import io.suggest.text.StringUtil

import scala.concurrent.Future
import scalaz.{EphemeralStream, NonEmptyList, Tree, Validation, ValidationNel}
import scalaz.syntax.apply._
import util.n2u.N2VldUtil
import util.tpl.HtmlSanitizer

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 10:15
 * Description: Общая утиль для работы с разными ad-формами: preview и обычными.
 */
@Singleton
class LkAdEdFormUtil @Inject() (
                                 n2VldUtil    : N2VldUtil,
                                 env          : Environment,
                               )
  extends MacroLogsImpl
{

  /** Макс.длина загружаемой картинки в байтах. */
  def IMG_UPLOAD_MAXLEN_BYTES: Int = {
    val mib = 40
    mib * 1024 * 1024
  }


  //---------------------------------------------------------------------------
  // v2 react form

  /** Срендерить и вернуть дефолтовый документ пустой карточки для текущего языка. */
  def defaultEmptyDocument(implicit ctx: Context): Future[MJdData] = {
    // TODO Брать готовую карточку из какого-то узла и пробегаться по эджам с использованием messages.

    // Тут просто очень временный документ.
    // Потом надо будет просто искать карточки на определённом узле, и возвращать их документ
    // и эджи (эджи - прорендерив через ctx.messages). Карточки сгенерить через новый редактор (который ещё не написан).

    val tplTree = Tree.Node(
      root = JdTag.document,
      forest = EphemeralStream(
        // Уровень стрипов. Рендерим три стрипа.

        // Strip#1 содержит намёк на то, что это верхний блок.
        Tree.Leaf {
          JdTag.block(
            bm = BlockMeta(
              w = BlockWidths.default,
              h = BlockHeights.default,
            ),
            bgColor = Some(MColorData(
              code = "060d45"
            ))
          )
        }

      )
    )

    val r = MJdData(
      doc = MJdDoc(
        template  = tplTree,
        tagId      = MJdTagId.empty,
      ),
      edges    = Nil,
      title    = None,
    )

    Future.successful(r)
  }


  /** Валидация данных файла, готовящегося к заливке.
    *
    * @param fileProps Присланные клиентом данные по файлу.
    * @return ValidationNel с выверенными данными или ошибкой.
    */
  def image4UploadPropsV(fileProps: MFileMeta): ValidationNel[String, MFileMeta] = {
    MFileMeta.validateUpload(
      m             = fileProps,
      // Бывает, что загружается просто png-рамка, например:
      minSizeB      = 256,
      maxSizeB      = IMG_UPLOAD_MAXLEN_BYTES,
      mimeVerifierF = ContentTypeCheck.OnlyImages,
      mustHashes    = UploadConstants.CleverUp.UPLOAD_FILE_HASHES,
    )
  }


  /** Запуск ранней синхронной валидации эджей, из присланной юзером JSON-формы.
    * Происходит базовая проверка данных.
    *
    * @param form JSON-форма с клиента.
    * @return Фьючерс с результатом валидации.
    *         exception обозначает ошибку валидации.
    */
  def earlyValidateJdData(form: MJdData): StringValidationNel[MJdData] = {
    val nodeIdVld = Validation.liftNel(form.doc.tagId.nodeId)(_.nonEmpty, "e.nodeid." + ErrorConstants.Words.UNEXPECTED)

    // Прочистить начальную карту эджей от возможного мусора (которого там быть и не должно, по идее).
    val edgesMap = form.edges
      .zipWithIdIter[EdgeUid_t]
      .to( Map )

    val edges1 = JdTag.purgeUnusedEdges( form.doc.template, edgesMap )

    // Ранняя валидация корректности присланных эджей:
    val edgesVlds = n2VldUtil.earlyValidateEdges( edges1.values )

    if (edgesVlds.isFailure)
      LOGGER.warn( s"earlyValidateEdges(${form.edges.size})[${System.currentTimeMillis()}]: Failed to validate edges: $edgesVlds\n edges = \n ${edges1.mkString("\n ")}" )

    (
      (nodeIdVld *> Validation.success[NonEmptyList[String], MJdDoc](form.doc)) |@|
      edgesVlds |@|
      // Заголовок: trim + limitLen
      ScalazUtil.liftNelOpt( form.title ) { title0 =>
        val titleSanitized = HtmlSanitizer.stripAllPolicy.sanitize( title0 )
        val title2 = StringUtil.strLimitLen( titleSanitized.trim, 100 )
        Validation.success[NonEmptyList[String], String]( title2 )
      } // Убрать пустые строки:
        .map(_.filter(_.nonEmpty))
    )( MJdData.apply )
  }


  /** Какие предикаты относятся к картинкам? */
  def IMAGE_PREDICATES = MPredicates.JdContent.Image :: Nil

  /** Произвести валидацию шаблона на стороне сервера. */
  def validateTpl(template       : Tree[JdTag],
                  vldEdgesMap    : Map[EdgeUid_t, MJdEdgeVldInfo]
                 ): StringValidationNel[Tree[JdTag]] = {
    lazy val logPrefix = s"validateTpl()[${System.currentTimeMillis()}]:"
    LOGGER.trace(s"$logPrefix Starting with ${vldEdgesMap.size} vld-edges.")

    val vldtor = new JdDocValidator(
      tolerant  = env.mode == Mode.Prod,
      edges     = vldEdgesMap,
    )
    val vldRes = vldtor.validateDocumentTree( template )

    LOGGER.trace(s"$logPrefix Validation completed => $vldRes")

    vldRes
  }


  def mkEdgeTextsMap(edges: IterableOnce[MJdEdge]): Map[EdgeUid_t, String] = {
    (for {
      e <- edges.iterator
      text <- e.edgeDoc.text
      edgeUid <- e.edgeDoc.id
    } yield {
      edgeUid -> text
    })
      .toMap
  }

  def mkTechName(tpl: Tree[JdTag], edgeTextsMap: Map[EdgeUid_t, String]): Option[String] = {
    val delim = " | "
    val techName = (for {
      (qdTree, i) <- EphemeralStream.toIterable(
        tpl
          .deepSubtrees
          .zipWithIndex
      )
        .iterator

      if qdTree.rootLabel.name ==* MJdTagNames.QD_CONTENT

      str <- {
        // Внутри одного qd-тега склеить все тексты без разделителей
        val edgeTexts0 = for {
          s <- qdTree.deepEdgesUids
          et <- edgeTextsMap.get(s)
          if et.trim.nonEmpty && et.length >= 2
        } yield {
          et.replace("\\s+", " ")
        }
        // Добавить разделитель, если требутся.
        if (i > 0 && delim.nonEmpty)
          delim #:: edgeTexts0
        else
          edgeTexts0
      }
    } yield str)
      .mkStringLimitLen(40)
      .trim

    OptionUtil.maybe(techName.length >= 2)(techName)
  }

}
