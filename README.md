# MapPoC

This repository contains code for Android application to move a cab marker on Google Maps Activity.

### Pushing Data

Location data (LatLng values) are pushed to the device using GCM downstream messages.

### Constraints
- Animate marker for the time that has elapsed from the last request
- If distance between two positions is less than 500m:
 -  Make request to Snap to Roads API
  - Calculate distance between each latlng received from the API
  - Animate them as per the distance to the time elapsed ratio
  - Rotation animation takes place first for 300 ms, if distance is less than 25 m
  - Movement animation takes place next for duration - 300 ms
  - This is repeated for each location returned by API
- Else if distance between two positions is greater than 500m:
 - Don't call Snap to Roads API
 - Animate it from current to new location
 
