package certifiedcarry_api.queue.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PendingQueueService {

  private final PendingRecruiterService pendingRecruiterService;
  private final PendingRankService pendingRankService;

  public PendingQueueService(
      PendingRecruiterService pendingRecruiterService, PendingRankService pendingRankService) {
    this.pendingRecruiterService = pendingRecruiterService;
    this.pendingRankService = pendingRankService;
  }

  public List<Map<String, Object>> getPendingRecruiters() {
    return pendingRecruiterService.getPendingRecruiters();
  }

  public List<Map<String, Object>> getPendingRecruitersForActor(
      long actorUserId, String backendUserRole, boolean isAdmin) {
    return pendingRecruiterService.getPendingRecruitersForActor(
        actorUserId, backendUserRole, isAdmin);
  }

  public Map<String, Object> createPendingRecruiterForActor(
      Map<String, Object> request, long actorUserId, String backendUserRole, boolean isAdmin) {
    return pendingRecruiterService.createPendingRecruiterForActor(
        request, actorUserId, backendUserRole, isAdmin);
  }

  public void deletePendingRecruiterForActor(
      String pendingRecruiterId, long actorUserId, String backendUserRole, boolean isAdmin) {
    pendingRecruiterService.deletePendingRecruiterForActor(
        pendingRecruiterId, actorUserId, backendUserRole, isAdmin);
  }

  public Map<String, Object> createPendingRecruiter(Map<String, Object> request) {
    return pendingRecruiterService.createPendingRecruiter(request);
  }

  public void deletePendingRecruiter(String pendingRecruiterId) {
    pendingRecruiterService.deletePendingRecruiter(pendingRecruiterId);
  }

  public List<Map<String, Object>> getPendingRanks(String status) {
    return pendingRankService.getPendingRanks(status);
  }

  public List<Map<String, Object>> getPendingRanksForActor(
      String status, long actorUserId, boolean isAdmin) {
    return pendingRankService.getPendingRanksForActor(status, actorUserId, isAdmin);
  }

  public List<Map<String, Object>> getPendingRanksForUser(String status, long userId) {
    return pendingRankService.getPendingRanksForUser(status, userId);
  }

  public OffsetDateTime getLatestDeclinedTimestampForUser(long userId) {
    return pendingRankService.getLatestDeclinedTimestampForUser(userId);
  }

  public void enforceDeclinedResubmissionCooldown(long actorUserId) {
    pendingRankService.enforceDeclinedResubmissionCooldown(actorUserId);
  }

  public boolean isPendingRankOwnedBy(String pendingRankId, long expectedUserId) {
    return pendingRankService.isPendingRankOwnedBy(pendingRankId, expectedUserId);
  }

  public Map<String, Object> createPendingRankForActor(
      Map<String, Object> request, long actorUserId, boolean isAdmin) {
    return pendingRankService.createPendingRankForActor(request, actorUserId, isAdmin);
  }

  public Map<String, Object> patchPendingRankForActor(
      String pendingRankId, Map<String, Object> request, long actorUserId, boolean isAdmin) {
    return pendingRankService.patchPendingRankForActor(
        pendingRankId, request, actorUserId, isAdmin);
  }

  public void deletePendingRankForActor(String pendingRankId, long actorUserId, boolean isAdmin) {
    pendingRankService.deletePendingRankForActor(pendingRankId, actorUserId, isAdmin);
  }

  public Map<String, Object> createPendingRank(Map<String, Object> request) {
    return pendingRankService.createPendingRank(request);
  }

  public Map<String, Object> patchPendingRank(String pendingRankId, Map<String, Object> request) {
    return pendingRankService.patchPendingRank(pendingRankId, request);
  }

  public void deletePendingRank(String pendingRankId) {
    pendingRankService.deletePendingRank(pendingRankId);
  }
}
