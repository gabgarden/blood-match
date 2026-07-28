db.donation_requests.updateMany(
  {
    $or: [
      { version: { $exists: false } },
      { version: null }
    ]
  },
  {
    $set: { version: NumberLong(0) }
  }
);

const locationUpdates = [];

db.donation_requests.find({ location: { $exists: false } }).forEach((request) => {
  const bloodCenter = db.blood_centers.findOne({ organizationId: request.bloodCenterId });
  if (bloodCenter == null) return;

  const organization = db.parties.findOne({
    _id: bloodCenter.organizationId,
    latitude: { $ne: null },
    longitude: { $ne: null }
  });
  if (organization == null) return;

  locationUpdates.push({
    updateOne: {
      filter: { _id: request._id },
      update: { $set: { location: [organization.longitude, organization.latitude] } }
    }
  });

  if (locationUpdates.length === 500) {
    db.donation_requests.bulkWrite(locationUpdates);
    locationUpdates.length = 0;
  }
});

if (locationUpdates.length > 0) {
  db.donation_requests.bulkWrite(locationUpdates);
}
