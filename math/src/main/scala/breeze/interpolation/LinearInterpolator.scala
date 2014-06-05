package breeze.interpolation

/**
 *
 * @author chrismedrela
 */

import scala.reflect.ClassTag

import breeze.generic.{UFunc, MappingUFunc, VariableUFunc}
import breeze.linalg._
import breeze.math.Field


object UnivariateInterpolatorImpl extends UFunc with MappingUFunc {
  /*implicit object impl[T] extends Impl2[UnivariateInterpolator[T],Double,Double] {
    def apply(k: UnivariateInterpolator[T], v: Double) = k(v)
  }*/
  implicit def impl[T]: Impl2[UnivariateInterpolator[T], T, T] = new Impl2[UnivariateInterpolator[T], T, T] {
    def apply(k: UnivariateInterpolator[T], v: T): T = k(v)
  }
}

trait UnivariateInterpolator[T] extends VariableUFunc[UnivariateInterpolatorImpl.type, UnivariateInterpolator[T]] {
  def apply(x: T): T
}

abstract class HandyUnivariateInterpolator[T:ClassTag:Field:Ordering]
    (x_coords: Vector[T],
     y_coords: Vector[T])
    extends UnivariateInterpolator[T] {

  if (x_coords.size != x_coords.toArray.toSet.size)
    throw new Exception("x coordinates must be unique")
  if (x_coords.size != y_coords.size)
    throw new Exception("x_coords and y_coords must be of the same size")
  if (x_coords.size == 0)
    throw new Exception("need to provide at least one pair of coordinates")

  private val nodes = x_coords.toArray zip y_coords.toArray sortBy {n => n._1}
  protected val X: Array[T] = nodes map {n => n._1}
  protected val Y: Array[T] = nodes map {n => n._2}

  private val ord = implicitly[Ordering[T]]
  import ord.mkOrderingOps

  def apply(x: T): T = {
    if (x < X(0) || x > X(X.size - 1))
      throw new IndexOutOfBoundsException("Out of the domain [" + X(0) + "," + X(X.size-1) + "]")

    valueAt(x)
  }

  protected def valueAt(b: T): T
}

class LinearInterpolator[T:ClassTag:Field:Ordering]
    (x_coords: Vector[T],
     y_coords: Vector[T])
    extends HandyUnivariateInterpolator[T](x_coords, y_coords) {

  private val ord = implicitly[Ordering[T]]
  import ord.mkOrderingOps

  override protected def valueAt(x: T): T = {
    def bisearch(low: Int, high: Int): Int = (low+high)/2 match {
      case mid if low == high => mid
      case mid if X(mid) < x => bisearch(mid+1, high)
      case mid => bisearch(low, mid)
    }
    val index = bisearch(0, X.length-1)

    val f = implicitly[Field[T]]
    // w = (x - x1) / (x2 - x1)
    val w = f./(f.-(x,
                    X(index-1)),
                f.-(X(index),
                    X(index-1)))
    // u = 1 - w
    val u = f.-(f.one, w)

    // result = y1 * u + y2 * w
    f.+(f.*(Y(index-1), u),
        f.*(Y(index), w))
  }
}

object LinearInterpolator {
  def apply[T:ClassTag:Field:Ordering](
     x_coords: Vector[T],
     y_coords: Vector[T]) = new LinearInterpolator(x_coords, y_coords)
}