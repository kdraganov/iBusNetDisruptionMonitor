package lbsl

/**
 * 1: Trip from the depot to the start stop of the block.
 * 2: Trip to a new starting point.
 * 3: Normal trip with passengers.
 * 4: Trip from the last stop of the block to the depot.
 * 5: Trip without passengers.
 * 6: Route Variant
 * 7: Vehicle is not logged in to either block or route (e.x sending from garage)
 *
 * Created by Konstantin on 26/01/2015.
 */
object TripType extends Enumeration {

  val FromDepotToStartPoint = 1
  val ToNewStartingPoint = 2
  val Normal = 3
  val ToDepot = 4
  val WithoutPassengers = 5
  val RouteVariant = 6
  val NotLogged = 7

  /**
   *
   * @param tripType Integer - the trip type to be checked if is active one
   * @return Boolean - true if the provided trip type is active, false otherwise
   */
  def isActiveTrip(tripType: Integer): Boolean = {
    if (tripType == 3 || tripType == 2) {
      return true
    }
    return false
  }

}