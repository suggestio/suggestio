package io.suggest.jd.tags.html

import io.suggest.common.empty.EmptyProduct
import io.suggest.err.ErrorConstants
import io.suggest.jd.{MJdEdgeId, MJdEdgeVldInfo}
import io.suggest.math.MathConst
import io.suggest.n2.edge.{EdgeUid_t, MPredicate, MPredicates}
import io.suggest.scalaz.{ScalazUtil, StringValidationNel}
import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.Validation
import scalaz.syntax.validation._
import scalaz.syntax.apply._
import scalaz.std.list._

object MJdHtmlTag {

  object Fields {
    final def TAG_NAME = "t"
    final def ATTRS = "a"
  }

  @inline implicit def univEq: UnivEq[MJdHtmlTag] = UnivEq.derive

  implicit def jdHtmlTagJson: OFormat[MJdHtmlTag] = {
    val F = Fields
    (
      (__ \ F.TAG_NAME).format[String] and
      (__ \ F.ATTRS).formatNullable[Map[String, MJdHtmlAttrValue]]
        .inmap[Map[String, MJdHtmlAttrValue]](
          _ getOrElse Map.empty,
          theMap => Option.when(theMap.nonEmpty)( theMap )
        )
    )(apply, unlift(unapply))
  }


  /** Validate single JD-HTML-tag info.
    *
    * @param tag HTML-tag info.
    * @param edgesMap Pre-validated edges map.
    * @return Validation result.
    */
  def validate(tag: MJdHtmlTag, edgesMap: Map[EdgeUid_t, MJdEdgeVldInfo]): StringValidationNel[MJdHtmlTag] = {
    (
      Validation.liftNel( tag.tagName )(
        tagName => !(tagName matches "[a-z0-9]{1,20}"),
        ErrorConstants.Words.INVALID,
      ) |@| {
        // Validate html tag attributes map:
        ScalazUtil
          .validateAll( tag.attrs.toList )( MJdHtmlAttrValue.validateKeyValue( _, edgesMap ).map(_ :: Nil) )
          .map(_.toMap)
      }
    )(apply)
  }

}
/** Data container for html-tag info inside JD-tag.
  *
  * @param tagName Html tag name.
  *                Lower-case, for example"img", "b" и т.д.
  * @param attrs HTML-tag attributes. Keys in lower-case.
  */
final case class MJdHtmlTag(
                             tagName     : String,
                             attrs       : Map[String, MJdHtmlAttrValue],
                           )


/** Statics for HTMLtag-in-JdTag HTML-attribute value. */
object MJdHtmlAttrValue {

  object Fields {
    final def VALUE = "v"
    final def EDGE_UID = "e"
  }

  @inline implicit def univEq: UnivEq[MJdHtmlAttrValue] = UnivEq.derive

  implicit def jdHtmlAvJson: OFormat[MJdHtmlAttrValue] = {
    val F = Fields
    (
      (__ \ F.VALUE).formatNullable[String] and
      (__ \ F.EDGE_UID).formatNullable[MJdEdgeId]
    )(apply, unlift(unapply))
  }


  /** Validation for html attribute key-value pair.
    *
    * @param htmlTagAttr Key-value tuple of JD-HTML-tag.
    * @param edgesMap Edges map.
    * @return Validation result.
    */
  def validateKeyValue(htmlTagAttr: (String, MJdHtmlAttrValue),
                       edgesMap: Map[EdgeUid_t, MJdEdgeVldInfo] ): StringValidationNel[(String, MJdHtmlAttrValue)] = {
    // TODO
    val attrName = htmlTagAttr._1
    (
      Validation.liftNel( attrName )(
        attrName => !(attrName matches "[a-z-]"),
        "attr name " + ErrorConstants.Words.INVALID + ": " + attrName
      ) |@|
        MJdHtmlAttrValue.validateValue(
          htmlTagAttr._2,
          edgesMap,
          // Predicate depends on html attribute name:
          predicate = Option[MPredicate] {
            attrName match {
              case "src" => MPredicates.JdContent.Image
              case "alt" | "title" => MPredicates.JdContent.Text
              case _ => null
            }
          },
        )
      )(_ -> _)
  }


  /** Validation for [[MJdHtmlAttrValue]] instance.
    *
    * @param attrValue Attr value data.
    * @param edgesMap Pre-validated edges map.
    * @param predicate Optional predicate.
    *                  None - means, value must be saved in raw [[MJdHtmlAttrValue.value]] field.
    *                  Some() means, value to be saved inside jd-edge.
    * @return
    */
  def validateValue(attrValue: MJdHtmlAttrValue,
                    edgesMap: Map[EdgeUid_t, MJdEdgeVldInfo],
                    predicate: Option[MPredicate]): StringValidationNel[MJdHtmlAttrValue] = {
    Validation
      .liftNel( attrValue )(
        m => m.isEmpty || (m.value.nonEmpty && m.edgeUid.nonEmpty),
        ErrorConstants.Words.EXPECTED
      )
      .andThen { _ =>
        (
          ScalazUtil.liftNelOptMust( attrValue.value, mustBeSome = predicate.isEmpty, ErrorConstants.Words.UNEXPECTED ) { valueStr =>
            MathConst.Counts.validateMinMax( valueStr.length, 0, 128, ErrorConstants.Words.TOO_MANY + "[" + valueStr.length + "]: " + valueStr )
              .map( _ => valueStr )
          } |@|
          predicate.fold {
            Option.empty[MJdEdgeId].successNel[String]
          } { pred =>
            ScalazUtil.liftNelOpt( attrValue.edgeUid ) { edgeUid =>
              pred match {
                case MPredicates.JdContent.Image =>
                  MJdEdgeId.validateImgId(edgeUid, edgesMap, imgContSzOpt = None)
                case MPredicates.JdContent.Ad =>
                  MJdEdgeId.validateAdId(edgeUid, edgesMap)
                case MPredicates.JdContent.Text =>
                  MJdEdgeId.validateText(edgeUid, edgesMap)
                // TODO case MPredicates.JdContent.Frame =>
                case _ =>
                  Validation.failureNel( "Unsupported predicate: " + predicate )
              }
            }
          }

        )( apply )
      }
  }

}
/** HTML attribute value container for each html-attribute in HTML-JD-tag.
  *
  * @param value Raw attribute string value.
  * @param edgeUid id эджа для доступа к значению аттрибута, когда value не задан.
  */
final case class MJdHtmlAttrValue(
                                   value      : Option[String]        = None,
                                   edgeUid    : Option[MJdEdgeId]     = None,
                                 )
  extends EmptyProduct
