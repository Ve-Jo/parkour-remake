package org.ayosynk.hubparkour.repository;

import org.ayosynk.hubparkour.db.DatabaseManager;
import org.ayosynk.hubparkour.model.Checkpoint;
import org.ayosynk.hubparkour.model.GlobalParkourTime;
import org.ayosynk.hubparkour.model.Leaderboard;
import org.ayosynk.hubparkour.model.Parkour;
import org.ayosynk.hubparkour.model.ParkourTime;
import org.ayosynk.hubparkour.util.LocationUtil;
import org.ayosynk.hubparkour.util.MaterialUtil;
import org.bukkit.Location;
import org.bukkit.Material;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ParkourRepository {
    private final DatabaseManager databaseManager;

    public ParkourRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Optional<Parkour> findParkourByName(String name) {
        String sql = "SELECT id, name, start_location, end_location FROM parkours WHERE name = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Parkour(
                            rs.getInt("id"),
                            rs.getString("name"),
                            LocationUtil.fromStorage(rs.getString("start_location")),
                            LocationUtil.fromStorage(rs.getString("end_location"))
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return Optional.empty();
    }

    public Optional<Parkour> findParkourById(int id) {
        String sql = "SELECT id, name, start_location, end_location FROM parkours WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Parkour(
                            rs.getInt("id"),
                            rs.getString("name"),
                            LocationUtil.fromStorage(rs.getString("start_location")),
                            LocationUtil.fromStorage(rs.getString("end_location"))
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return Optional.empty();
    }

    public List<Parkour> listParkours() {
        List<Parkour> parkours = new ArrayList<>();
        String sql = "SELECT id, name, start_location, end_location FROM parkours ORDER BY name";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                parkours.add(new Parkour(
                        rs.getInt("id"),
                        rs.getString("name"),
                        LocationUtil.fromStorage(rs.getString("start_location")),
                        LocationUtil.fromStorage(rs.getString("end_location"))
                ));
            }
        } catch (SQLException ignored) {
        }
        return parkours;
    }

    public boolean createParkour(String name, Location startLocation, Location endLocation, Material startMaterial, Material endMaterial) {
        String sql = "INSERT INTO parkours (name, start_location, start_material, end_location, end_material) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, LocationUtil.toStorage(startLocation));
            stmt.setString(3, materialId(startMaterial));
            stmt.setString(4, LocationUtil.toStorage(endLocation));
            stmt.setString(5, materialId(endMaterial));
            return stmt.executeUpdate() > 0;
        } catch (SQLException ignored) {
        }
        return false;
    }

    public boolean updateStart(int parkourId, Location location, Material material) {
        String sql = "UPDATE parkours SET start_location = ?, start_material = ? WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, LocationUtil.toStorage(location));
            stmt.setString(2, materialId(material));
            stmt.setInt(3, parkourId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException ignored) {
        }
        return false;
    }

    public boolean updateEnd(int parkourId, Location location, Material material) {
        String sql = "UPDATE parkours SET end_location = ?, end_material = ? WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, LocationUtil.toStorage(location));
            stmt.setString(2, materialId(material));
            stmt.setInt(3, parkourId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException ignored) {
        }
        return false;
    }

    public boolean updateName(int parkourId, String newName) {
        String sql = "UPDATE parkours SET name = ? WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newName);
            stmt.setInt(2, parkourId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException ignored) {
        }
        return false;
    }

    public boolean deleteParkour(int parkourId) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM leaderboard_lines WHERE leaderboard_id IN (SELECT id FROM leaderboards WHERE parkour_id = ?)");
                 PreparedStatement deleteLeaderboards = connection.prepareStatement("DELETE FROM leaderboards WHERE parkour_id = ?");
                 PreparedStatement deleteTimes = connection.prepareStatement("DELETE FROM parkour_times WHERE parkour_id = ?");
                 PreparedStatement deleteCheckpoints = connection.prepareStatement("DELETE FROM checkpoints WHERE parkour_id = ?");
                 PreparedStatement deleteParkour = connection.prepareStatement("DELETE FROM parkours WHERE id = ?")) {
                stmt.setInt(1, parkourId);
                stmt.executeUpdate();
                deleteLeaderboards.setInt(1, parkourId);
                deleteLeaderboards.executeUpdate();
                deleteTimes.setInt(1, parkourId);
                deleteTimes.executeUpdate();
                deleteCheckpoints.setInt(1, parkourId);
                deleteCheckpoints.executeUpdate();
                deleteParkour.setInt(1, parkourId);
                boolean deleted = deleteParkour.executeUpdate() > 0;
                connection.commit();
                return deleted;
            } catch (SQLException ex) {
                connection.rollback();
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    public List<Checkpoint> getCheckpoints(int parkourId) {
        List<Checkpoint> checkpoints = new ArrayList<>();
        String sql = "SELECT id, parkour_id, cp_index, location, material FROM checkpoints WHERE parkour_id = ? ORDER BY cp_index";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parkourId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    checkpoints.add(new Checkpoint(
                            rs.getInt("id"),
                            rs.getInt("parkour_id"),
                            rs.getInt("cp_index"),
                            LocationUtil.fromStorage(rs.getString("location")),
                            MaterialUtil.fromId(rs.getString("material"), Material.STONE)
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return checkpoints;
    }

    public Optional<Checkpoint> getCheckpointByLocation(String locationKey) {
        String sql = "SELECT id, parkour_id, cp_index, location, material FROM checkpoints WHERE location = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, locationKey);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Checkpoint(
                            rs.getInt("id"),
                            rs.getInt("parkour_id"),
                            rs.getInt("cp_index"),
                            LocationUtil.fromStorage(rs.getString("location")),
                            MaterialUtil.fromId(rs.getString("material"), Material.STONE)
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return Optional.empty();
    }

    public Optional<Location> getCheckpointLocation(int parkourId, int index) {
        String sql = "SELECT location FROM checkpoints WHERE parkour_id = ? AND cp_index = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parkourId);
            stmt.setInt(2, index);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(LocationUtil.fromStorage(rs.getString("location")));
                }
            }
        } catch (SQLException ignored) {
        }
        return Optional.empty();
    }

    public int getMaxCheckpointIndex(int parkourId) {
        String sql = "SELECT MAX(cp_index) AS max_index FROM checkpoints WHERE parkour_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parkourId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("max_index");
                }
            }
        } catch (SQLException ignored) {
        }
        return 0;
    }

    public boolean createCheckpoint(int parkourId, int index, Location location, Material material) {
        String sql = "INSERT INTO checkpoints (parkour_id, cp_index, location, material) VALUES (?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parkourId);
            stmt.setInt(2, index);
            stmt.setString(3, LocationUtil.toStorage(location));
            stmt.setString(4, materialId(material));
            return stmt.executeUpdate() > 0;
        } catch (SQLException ignored) {
        }
        return false;
    }

    public boolean deleteCheckpoint(int parkourId, int index) {
        String sql = "DELETE FROM checkpoints WHERE parkour_id = ? AND cp_index = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parkourId);
            stmt.setInt(2, index);
            return stmt.executeUpdate() > 0;
        } catch (SQLException ignored) {
        }
        return false;
    }

    public void saveTime(UUID playerUuid, int parkourId, double timeSeconds) {
        String sql = "INSERT INTO parkour_times (player_uuid, parkour_id, comp_time) VALUES (?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setInt(2, parkourId);
            stmt.setDouble(3, timeSeconds);
            stmt.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public Optional<Double> getPersonalBest(UUID playerUuid, int parkourId) {
        String sql = "SELECT MIN(comp_time) AS best_time FROM parkour_times WHERE player_uuid = ? AND parkour_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setInt(2, parkourId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double value = rs.getDouble("best_time");
                    if (!rs.wasNull()) {
                        return Optional.of(value);
                    }
                }
            }
        } catch (SQLException ignored) {
        }
        return Optional.empty();
    }

    public List<ParkourTime> getBestTimes(int parkourId, int limit) {
        List<ParkourTime> times = new ArrayList<>();
        String sql = "SELECT player_uuid, MIN(comp_time) AS best_time FROM parkour_times WHERE parkour_id = ? GROUP BY player_uuid ORDER BY best_time ASC LIMIT ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parkourId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    times.add(new ParkourTime(UUID.fromString(rs.getString("player_uuid")), rs.getDouble("best_time")));
                }
            }
        } catch (SQLException ignored) {
        }
        return times;
    }

    public List<GlobalParkourTime> getGlobalBestTimes(int limit) {
        List<GlobalParkourTime> times = new ArrayList<>();
        String sql = "SELECT parkour_id, player_uuid, MIN(comp_time) AS best_time "
                + "FROM parkour_times GROUP BY parkour_id, player_uuid "
                + "ORDER BY best_time ASC LIMIT ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    times.add(new GlobalParkourTime(
                            rs.getInt("parkour_id"),
                            UUID.fromString(rs.getString("player_uuid")),
                            rs.getDouble("best_time")
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return times;
    }

    public Optional<Double> getBestTime(int parkourId) {
        String sql = "SELECT MIN(comp_time) AS best_time FROM parkour_times WHERE parkour_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parkourId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double value = rs.getDouble("best_time");
                    if (!rs.wasNull()) {
                        return Optional.of(value);
                    }
                }
            }
        } catch (SQLException ignored) {
        }
        return Optional.empty();
    }

    public Optional<Leaderboard> createLeaderboard(int parkourId, Location location) {
        String sql = "INSERT INTO leaderboards (parkour_id, location) VALUES (?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, parkourId);
            stmt.setString(2, LocationUtil.toStorage(location));
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return Optional.of(new Leaderboard(rs.getInt(1), parkourId, location));
                }
            }
        } catch (SQLException ignored) {
        }
        return Optional.empty();
    }

    public List<Leaderboard> listLeaderboards() {
        List<Leaderboard> boards = new ArrayList<>();
        String sql = "SELECT id, parkour_id, location FROM leaderboards";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                boards.add(new Leaderboard(
                        rs.getInt("id"),
                        rs.getInt("parkour_id"),
                        LocationUtil.fromStorage(rs.getString("location"))
                ));
            }
        } catch (SQLException ignored) {
        }
        return boards;
    }

    public void deleteLeaderboard(int leaderboardId) {
        try (Connection connection = databaseManager.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM leaderboard_lines WHERE leaderboard_id = ?")) {
                stmt.setInt(1, leaderboardId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM leaderboards WHERE id = ?")) {
                stmt.setInt(1, leaderboardId);
                stmt.executeUpdate();
            }
        } catch (SQLException ignored) {
        }
    }

    public void deleteLeaderboardLines(int leaderboardId) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement("DELETE FROM leaderboard_lines WHERE leaderboard_id = ?")) {
            stmt.setInt(1, leaderboardId);
            stmt.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public void storeLeaderboardLine(int leaderboardId, int position, String hologramName, Location location) {
        String sql = "INSERT INTO leaderboard_lines (leaderboard_id, line_position, hologram_name, location) VALUES (?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, leaderboardId);
            stmt.setInt(2, position);
            stmt.setString(3, hologramName);
            stmt.setString(4, LocationUtil.toStorage(location));
            stmt.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public List<String> getLeaderboardLineNames(int leaderboardId) {
        List<String> lines = new ArrayList<>();
        String sql = "SELECT hologram_name FROM leaderboard_lines WHERE leaderboard_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, leaderboardId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lines.add(rs.getString("hologram_name"));
                }
            }
        } catch (SQLException ignored) {
        }
        return lines;
    }

    private String materialId(Material material) {
        if (material == null) {
            return "minecraft:stone";
        }
        return "minecraft:" + material.name().toLowerCase();
    }
}
