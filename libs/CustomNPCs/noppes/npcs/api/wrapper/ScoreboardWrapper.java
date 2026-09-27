package noppes.npcs.api.wrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IScoreboard;
import noppes.npcs.api.IScoreboardObjective;
import noppes.npcs.api.IScoreboardTeam;

public class ScoreboardWrapper implements IScoreboard {
   private Scoreboard board;
   private MinecraftServer server;

   protected ScoreboardWrapper(MinecraftServer server) {
      this.server = server;
      this.board = server.m_129880_(Level.f_46428_).m_6188_();
   }

   @Override
   public IScoreboardObjective[] getObjectives() {
      List<Objective> collection = new ArrayList<>(this.board.m_83466_());
      IScoreboardObjective[] objectives = new IScoreboardObjective[collection.size()];

      for (int i = 0; i < collection.size(); i++) {
         objectives[i] = new ScoreboardObjectiveWrapper(this.board, collection.get(i));
      }

      return objectives;
   }

   @Override
   public String[] getPlayerList() {
      Collection<String> collection = this.board.m_83474_();
      return collection.toArray(new String[collection.size()]);
   }

   @Override
   public IScoreboardObjective getObjective(String name) {
      Objective obj = this.board.m_83477_(name);
      return obj == null ? null : new ScoreboardObjectiveWrapper(this.board, obj);
   }

   @Override
   public boolean hasObjective(String objective) {
      return this.board.m_83477_(objective) != null;
   }

   @Override
   public void removeObjective(String objective) {
      Objective obj = this.board.m_83477_(objective);
      if (obj != null) {
         this.board.m_83502_(obj);
      }
   }

   @Override
   public IScoreboardObjective addObjective(String objective, String criteria) {
      ObjectiveCriteria icriteria = (ObjectiveCriteria)ObjectiveCriteria.m_83614_(criteria).orElse(null);
      if (icriteria == null) {
         throw new CustomNPCsException("Unknown score criteria: %s", criteria);
      } else if (objective.length() > 0 && objective.length() <= 16) {
         Objective obj = this.board.m_83436_(objective, icriteria, Component.m_237115_(objective), RenderType.INTEGER);
         return new ScoreboardObjectiveWrapper(this.board, obj);
      } else {
         throw new CustomNPCsException("Score objective must be between 1-16 characters: %s", objective);
      }
   }

   @Override
   public void setPlayerScore(String player, String objective, int score) {
      Objective objec = this.getObjectiveWithException(objective);
      if (!objec.m_83321_().m_83621_() && score >= Integer.MIN_VALUE && score <= Integer.MAX_VALUE) {
         Score sco = this.board.m_83471_(player, objec);
         sco.m_83402_(score);
      }
   }

   private Objective getObjectiveWithException(String objective) {
      Objective objec = this.board.m_83477_(objective);
      if (objec == null) {
         throw new CustomNPCsException("Score objective does not exist: %s", objective);
      } else {
         return objec;
      }
   }

   @Override
   public int getPlayerScore(String player, String objective) {
      Objective objec = this.getObjectiveWithException(objective);
      return objec.m_83321_().m_83621_() ? 0 : this.board.m_83471_(player, objec).m_83400_();
   }

   @Override
   public boolean hasPlayerObjective(String player, String objective) {
      Objective objec = this.getObjectiveWithException(objective);
      return this.board.m_83483_(player).get(objec) != null;
   }

   @Override
   public void deletePlayerScore(String player, String objective) {
      Objective objec = this.getObjectiveWithException(objective);
      if (this.board.m_83483_(player).remove(objec) != null) {
         this.board.m_83495_(player);
      }
   }

   @Override
   public IScoreboardTeam[] getTeams() {
      List<PlayerTeam> list = new ArrayList<>(this.board.m_83491_());
      IScoreboardTeam[] teams = new IScoreboardTeam[list.size()];

      for (int i = 0; i < list.size(); i++) {
         teams[i] = new ScoreboardTeamWrapper(list.get(i), this.board);
      }

      return teams;
   }

   @Override
   public boolean hasTeam(String name) {
      return this.board.m_83489_(name) != null;
   }

   @Override
   public IScoreboardTeam addTeam(String name) {
      if (this.hasTeam(name)) {
         throw new CustomNPCsException("Team %s already exists", name);
      } else {
         return new ScoreboardTeamWrapper(this.board.m_83492_(name), this.board);
      }
   }

   @Override
   public IScoreboardTeam getTeam(String name) {
      PlayerTeam team = this.board.m_83489_(name);
      return team == null ? null : new ScoreboardTeamWrapper(team, this.board);
   }

   @Override
   public void removeTeam(String name) {
      PlayerTeam team = this.board.m_83489_(name);
      if (team != null) {
         this.board.m_83475_(team);
      }
   }

   @Override
   public IScoreboardTeam getPlayerTeam(String player) {
      PlayerTeam team = this.board.m_83500_(player);
      return team == null ? null : new ScoreboardTeamWrapper(team, this.board);
   }

   @Override
   public void removePlayerTeam(String player) {
      this.board.m_83495_(player);
   }
}
