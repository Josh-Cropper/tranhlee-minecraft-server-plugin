package com.roll6.tranhlee

import com.roll6.tranhlee.auth.discord.Authentication
import com.roll6.tranhlee.commands.Auth
import com.roll6.tranhlee.commands.DadJoke
import com.roll6.tranhlee.entities.player.PlayerRepository
import com.roll6.tranhlee.listeners.ChatListener
import com.roll6.tranhlee.listeners.PlayerJoinListener
import com.roll6.tranhlee.manager.RepositoryManager
import com.roll6.tranhlee.manager.ResourceManager
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.Bukkit
import org.hibernate.service.spi.ServiceException
import javax.persistence.Persistence

class Main: JavaPlugin() {
    override fun onEnable() {
        Thread.currentThread().contextClassLoader = Main::class.java.classLoader

        ResourceManager.setResource(Main::class, this)
        this.saveDefaultConfig()

        try {
            ResourceManager.setResource(RepositoryManager::class, RepositoryManager(
                Persistence.createEntityManagerFactory(
                    "Minecraft",
                    mapOf(
                        Pair(
                            "javax.persistence.jdbc.url",
                            "jdbc:mysql://${this.config.getString("database.url", "localhost")}:" +
                                "${this.config.getInt("database.port", 3306)}/" +
                                "${this.config.getString("database.schema")}"
                        ),
                        Pair("javax.persistence.jdbc.user", this.config.getString("database.username")),
                        Pair("javax.persistence.jdbc.password", this.config.getString("database.password")?.trim()),
                    )
                ).createEntityManager()
            ))

            ResourceManager.setResource(Authentication::class, Authentication())
            val repositoryManager: RepositoryManager = ResourceManager.getResource(RepositoryManager::class)

            Bukkit.getServer().pluginManager.registerEvents(
                PlayerJoinListener(
                    repositoryManager
                        .getRepository(PlayerRepository::class.java),
                ),
                this
            )

            Bukkit.getServer().pluginManager.registerEvents(
                ChatListener(
                    repositoryManager
                        .getRepository(PlayerRepository::class.java),
                ),
                this
            )

            this.getCommand("dadjoke")?.setExecutor(DadJoke())
            this.getCommand("auth")?.setExecutor(Auth(repositoryManager.getRepository(PlayerRepository::class.java)))

            val minutes = 30L
            this.server.scheduler.scheduleSyncRepeatingTask(
                this,
                { ResourceManager.getResource<Authentication>(Authentication::class).endDiscordAuthentication() },
                0,
                (60 * minutes) * 20
            )
        } catch (exception: ServiceException) {
            return
        }
    }

    override fun onDisable() {
        ResourceManager.getResource<Authentication>(Authentication::class).endDiscordAuthentication()
    }
}