import { RouterProvider } from "react-router-dom";
import { router } from "./routes";
import { AuthProvider } from "./context/AuthContext";
import { PwaInstallPrompt } from "./components/ui/PwaInstallPrompt";

function App() {
  return (
    <AuthProvider>
      <RouterProvider router={router} />
      <PwaInstallPrompt />
    </AuthProvider>
  );
}

export default App;