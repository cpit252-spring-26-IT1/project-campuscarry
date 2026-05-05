import { Alert } from "@heroui/react";

const FormErrorAlert = ({ message }) => {
  if (!message) {
    return null;
  }

  return (
    <Alert
      className="mb-4"
      color="danger"
      description={message}
    />
  );
};

export default FormErrorAlert;
