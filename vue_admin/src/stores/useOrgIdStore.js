import { useOrganizationStore } from '@/stores/organizationStore.js';

export function useOrgIdStore() {
    const organizationStore = useOrganizationStore();
    return {
        get orgId() {
            return organizationStore.currentOrganizationId;
        },
    };
}