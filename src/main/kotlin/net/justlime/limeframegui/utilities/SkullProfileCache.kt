package net.justlime.limeframegui.utilities

object SkullProfileCache {
    // The cache stores either a PlayerProfile or a GameProfile, so we use 'Any'.
    private val cache = mutableMapOf<String, Any>()

    /**
     * Retrieves a profile object from the cache or creates a new one.
     *
     * @param texture The texture identifier.
     * @return The cached or newly created profile object (PlayerProfile on 1.18+, GameProfile on older versions).
     */
    fun getProfile(texture: String): Any {
        return cache.getOrPut(texture) {
            // Check the server version and create the correct type of profile.
            if (SkullUtils.VersionHelper.HAS_PLAYER_PROFILES) {
                SkullUtils.createProfileFromTexture(texture)
            } else {
                SkullUtils.createLegacyGameProfile(texture)
            }
        }
    }
}