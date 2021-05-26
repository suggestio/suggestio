package io.suggest

import org.elasticsearch.common.geo.builders.ShapeBuilder
import org.elasticsearch.geometry.Geometry
import org.locationtech.spatial4j.shape.Shape

package object geo {

  /** Elasticsearch-6.x+ requires generic-types for ShapeBuilder.
    * Here is workaround, to safely ignore all these generics. */
  type AnyEsShapeBuilder_t = ShapeBuilder[S, G, E] forSome { type S <: Shape; type G <: Geometry; type E <: ShapeBuilder[S, G, E] }

}
