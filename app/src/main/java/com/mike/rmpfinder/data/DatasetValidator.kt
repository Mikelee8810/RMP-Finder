package com.mike.rmpfinder.data

data class ValidatedDataset(
    val manifest: DatasetManifest,
    val restaurants: List<RmpRestaurant>,
    val entities: List<RestaurantEntity>,
    val missingExistingKeys: List<String>,
)

object DatasetValidator {
    fun validate(
        manifestBytes: ByteArray,
        datasetBytes: ByteArray,
        appVersion: Int,
        existingKeys: Set<String> = emptySet(),
    ): ValidatedDataset {
        val manifest = DatasetCodec.parseManifest(manifestBytes)
        require(manifest.schemaVersion == RmpRepository.SUPPORTED_SCHEMA_VERSION) { "Unsupported dataset schema" }
        require(manifest.minimumAppVersion <= appVersion) { "This dataset needs a newer app version" }
        require(manifest.sha256 == DatasetCodec.sha256(datasetBytes)) { "Dataset checksum mismatch" }
        val (restaurants, entities) = DatasetCodec.entitiesFromDataset(datasetBytes)
        require(restaurants.size == manifest.recordCount) { "Dataset record count mismatch" }
        val newKeys = restaurants.map { it.rmpKey }.toSet()
        val missing = (existingKeys - newKeys).sorted()
        return ValidatedDataset(manifest, restaurants, entities, missing)
    }
}
