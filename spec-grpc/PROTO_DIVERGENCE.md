# Protobuf Schema Divergence from Upstream

This document tracks intentional divergences from the upstream A2A protobuf schema at https://github.com/a2aproject/A2A/blob/main/specification/grpc/a2a.proto

## Current Divergences

### ListTasksResponse.page_size (Field #3)

**Status**: ✅ Aligned with PR #1160 (v1.0 RC)
**Upstream PR**: https://github.com/a2aproject/A2A/pull/1160
**Reason**: TCK tests require `pageSize` field in responses per A2A v0.4.0 spec. PR #1160 adds this field to the schema.
**Action Required**: Remove this divergence once PR #1160 is merged and we sync to v1.0 RC.
**Impact**: Field number change - `total_size` moved from #3 to #4 to accommodate `page_size` at #3.

**Modified**: 2025-11-12
**Tracking Issue**: https://github.com/a2aproject/A2A/pull/1160

## Sync Instructions

When PR #1160 is merged:
1. Verify field numbers match our implementation
2. Remove this divergence note
3. Sync to upstream v1.0 RC tag
4. Regenerate protobuf classes: `cd spec-grpc && mvn clean install -Pproto-compile`
