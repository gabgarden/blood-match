import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { hasAdminRole, hasBloodCenterRole, hasDonorRole, hasRequesterRole } from "../../routes/roleRouting";
import type { DashboardNavItem } from "../../types/dashboardNav";

type MobileBottomNavProps = {
  activeItem?: DashboardNavItem;
};

type MenuItem = {
  key: DashboardNavItem;
  icon: string;
  label: string;
  path: string;
};

export function MobileBottomNav({ activeItem }: MobileBottomNavProps) {
  const location = useLocation();
  const { roles } = useAuth();
  const canAccessRequesterArea = hasRequesterRole(roles) || hasAdminRole(roles);
  const canAccessDonorDashboard = hasDonorRole(roles) || hasAdminRole(roles);
  const canAccessBloodCenter = hasBloodCenterRole(roles) || hasAdminRole(roles);

  const menuItems: MenuItem[] = [];

  if (canAccessDonorDashboard) {
    menuItems.push({ key: "donor-dashboard", icon: "home_health", label: "Central", path: "/dashboard" });
    menuItems.push({
      key: "recommendations",
      icon: "volunteer_activism",
      label: "Requisições",
      path: "/dashboard/recommendations",
    });
  }

  if (canAccessBloodCenter) {
    menuItems.push({ key: "blood-center", icon: "local_hospital", label: "Hemocentro", path: "/blood-center" });
  }

  if (canAccessRequesterArea && menuItems.length < 4) {
    menuItems.push({ key: "requests", icon: "assignment", label: "Suas Req.", path: "/requests" });
  }

  if (canAccessDonorDashboard && menuItems.length < 4) {
    menuItems.push({ key: "donations", icon: "water_drop", label: "Doações", path: "/donations" });
  }

  if (canAccessRequesterArea && menuItems.length < 4) {
    menuItems.push({ key: "new-request", icon: "add_circle", label: "Nova", path: "/requests/new" });
  }

  menuItems.push({ key: "profile", icon: "person", label: "Perfil", path: "/profile" });

  return (
    <nav
      className="lg:hidden fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur-xl border-t border-surface-container-high px-2 py-1.5 shadow-2xl flex items-center justify-around"
      aria-label="Navegação inferior mobile"
    >
      {menuItems.map((item) => {
        const isActive = activeItem === item.key || location.pathname === item.path;
        return (
          <Link
            key={item.key}
            to={item.path}
            className={`flex flex-col items-center justify-center flex-1 py-1 px-1 rounded-xl transition-all duration-200 text-center ${
              isActive
                ? "text-primary font-bold bg-red-50/80 scale-105"
                : "text-secondary hover:text-primary hover:bg-gray-50 font-medium"
            }`}
          >
            <span className={`material-symbols-outlined text-xl ${isActive ? "text-primary fill-1" : ""}`}>
              {item.icon}
            </span>
            <span className="text-[10px] mt-0.5 tracking-tight truncate max-w-full">{item.label}</span>
          </Link>
        );
      })}
    </nav>
  );
}
