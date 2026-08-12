import { createBrowserRouter } from "react-router-dom";
import DonorDashboardPage from "../pages/DashboardPage";
import LoginPage from "../pages/LoginPage";
import RequestsPage from "../pages/RequestsPage";
import NewRequestPage from "../pages/NewRequestPage";
import RegisterPage from "../pages/RegisterPage";
import RecommendationsPage from "../pages/RecommendationsPage";
import ExternalDonationPage from "../pages/ExternalDonationPage";
import ProfilePage from "../pages/ProfilePage";
import DonationsPage from "../pages/DonationsPage";
import { PublicOnlyRoute, RequireAuth, RoleBasedHomeRedirect } from "./RouteGuards";
import { RouteErrorPage } from "../components/ui/RouteErrorPage";

export const router = createBrowserRouter([
  {
    path: "/",
    errorElement: <RouteErrorPage />,
    element: (
      <RequireAuth>
        <RoleBasedHomeRedirect />
      </RequireAuth>
    ),
  },
  {
    path: "/login",
    errorElement: <RouteErrorPage />,
    element: (
      <PublicOnlyRoute>
        <LoginPage />
      </PublicOnlyRoute>
    ),
  },
  {
    path: "/register",
    errorElement: <RouteErrorPage />,
    element: (
      <PublicOnlyRoute>
        <RegisterPage />
      </PublicOnlyRoute>
    ),
  },
  {
    path: "/dashboard",
    errorElement: <RouteErrorPage />,
    element: (
      <RequireAuth>
        <DonorDashboardPage />
      </RequireAuth>
    ),
  },
  {
    path: "/dashboard/recommendations",
    errorElement: <RouteErrorPage />,
    element: (
      <RequireAuth>
        <RecommendationsPage />
      </RequireAuth>
    ),
  },
  {
    path: "/donations",
    errorElement: <RouteErrorPage />,
    element: (
      <RequireAuth>
        <DonationsPage />
      </RequireAuth>
    ),
  },
  {
    path: "/donations/external/new",
    errorElement: <RouteErrorPage />,
    element: (
      <RequireAuth>
        <ExternalDonationPage />
      </RequireAuth>
    ),
  },
  {
    path: "/requests",
    errorElement: <RouteErrorPage />,
    element: (
      <RequireAuth>
        <RequestsPage />
      </RequireAuth>
    ),
  },
  {
    path: "/requests/new",
    errorElement: <RouteErrorPage />,
    element: (
      <RequireAuth>
        <NewRequestPage />
      </RequireAuth>
    ),
  },
  {
    path: "/profile",
    errorElement: <RouteErrorPage />,
    element: (
      <RequireAuth>
        <ProfilePage />
      </RequireAuth>
    ),
  },
], {
  basename: import.meta.env.BASE_URL,
});